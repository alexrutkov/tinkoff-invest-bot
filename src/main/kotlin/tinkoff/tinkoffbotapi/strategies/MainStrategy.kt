package tinkoff.tinkoffbotapi.strategies

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import ru.tinkoff.piapi.contract.v1.OrderDirection
import ru.tinkoff.piapi.contract.v1.Share
import ru.tinkoff.piapi.core.MarketDataService
import ru.tinkoff.piapi.core.OperationsService
import ru.tinkoff.piapi.core.OrdersService
import tinkoff.tinkoffbotapi.events.StrategyBuyEvent
import tinkoff.tinkoffbotapi.extensions.toDecimal
import tinkoff.tinkoffbotapi.extensions.toQuotation
import tinkoff.tinkoffbotapi.model.AccountStrategy
import tinkoff.tinkoffbotapi.model.BuyOperation
import tinkoff.tinkoffbotapi.repositories.AccountOrderRepository
import tinkoff.tinkoffbotapi.repositories.StrategyRepository
import tinkoff.tinkoffbotapi.services.OrderInvestService
import tinkoff.tinkoffbotapi.services.OrderTradingService
import java.math.BigDecimal
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.BiPredicate

private val logger = KotlinLogging.logger {}
@Component
class MainStrategy(
	private val strategyRepository: StrategyRepository,
	private val orderInvestService: OrderInvestService,
	private val tradingService: OrderTradingService,
	private val marketDataService: MarketDataService,
	private val accountOrderRepository: AccountOrderRepository,
	private val ordersService: OrdersService,
	private val operationsService: OperationsService,
	private val shareCache: LoadingCache<UUID, Share>
)  {

	private val profit = BigDecimal.valueOf(0.01)
	private val incrementPrice = BigDecimal.valueOf(0.005)
	private val commission = BigDecimal.valueOf(0.0005)
	private val limitOrderSize = 2

	private val strategySource = Sinks.unsafe().many().replay().limit<AccountStrategy>(Duration.ofMinutes(10))
	private final val strategies = strategySource.asFlux()

	private val cache: Cache<Long, AccountStrategy> = Caffeine.newBuilder()
		.expireAfterWrite(4, TimeUnit.MINUTES)
		.build()

	private val predicateFn = BiPredicate<Cache<Long, AccountStrategy>, AccountStrategy> { c, value ->
		val key = value.id
		if (c.getIfPresent(key) != null) {
			false
		} else {
			c.put(key, value)
			true
		}
	}

	init {
		strategies
			.distinct({ it }, { cache }, predicateFn, { cache.cleanUp() })
			.delayElements(Duration.ofSeconds(5))
			.subscribe(::run)
	}

	@EventListener(StrategyBuyEvent::class)
	fun handleBuyEvent(event: StrategyBuyEvent) {
		this.createSoldOrder(event.strategy)
	}

	fun addStrategy(strategy: AccountStrategy) {
		strategySource.tryEmitNext(strategy)
	}

	private fun run(strategy: AccountStrategy) {
		val tradingStatus = marketDataService.getTradingStatusSync(strategy.instrumentId.toString())
		tradingService.refreshOrderStatesByInstruments(listOf(strategy.instrumentId))
		if (tradingStatus.limitOrderAvailableFlag) {
			createSoldOrder(strategy)
			createBuyOrders(strategy)
		}
	}

	private fun createBuyOrders(strategy: AccountStrategy) {
		val money = operationsService.getPositionsSync(strategy.accountId.toString()).money
			.find { it.currency == "rub" }?.value ?: BigDecimal.ZERO
		val share = shareCache.get(strategy.instrumentId)


		val orderBook = marketDataService.getOrderBookSync(strategy.instrumentId.toString(), 50)
		val currentOrders = accountOrderRepository.findPendingOrdersByInstrument(strategy.instrumentId)
			.filter { it.direction == OrderDirection.ORDER_DIRECTION_BUY }
		val buyPositions = strategyRepository.findPendingBuyOperations(strategy.id)
			.map { it.buyOrder.initialSecurityPrice.toDecimal() }
			.map(::mapToRange)
		val currentPrices = currentOrders.map { it.initialSecurityPrice.toDecimal() }
			.map(::mapToRange)
		val bidsPrice =  orderBook.bidsList.map { it.price.toDecimal() }.sortedDescending()
		if (bidsPrice.isNotEmpty() && money > bidsPrice.first().times(share.lot.toBigDecimal())) {
			bidsPrice
				.filterNot { price -> currentPrices.any { it.contains(price)  } }
				.filterNot { price -> buyPositions.any { it.contains(price)  } }
				.let {
					it.firstOrNull()
						?.let { lastPrice -> findMinPrice(lastPrice, it).plus(lastPrice) }
						?: emptyList()
				}.sortedDescending()
				.map(BigDecimal::toQuotation)
				.distinct()
				.take(limitOrderSize - currentOrders.size)
				.toList()
				.forEach { orderInvestService.createOrder(strategy, it) }
		}


	}

	private fun mapToRange(it: BigDecimal): ClosedRange<BigDecimal> {
		val delta = it.times(incrementPrice)
		return it.minus(delta)..it.plus(delta)
	}

	private fun findMinPrice(lastPrice: BigDecimal, all: List<BigDecimal>): List<BigDecimal> {
		val delta = lastPrice.times(incrementPrice)
		val minPrice = lastPrice.minus(delta)
		return all.firstOrNull { minPrice > it }
			?.let { price -> listOf(price).plus(findMinPrice(price, all.filter { it < price })) }
			?: emptyList()
	}



	private fun createSoldOrder(strategy: AccountStrategy) {


			val currentOrders = ordersService.getOrdersSync(strategy.accountId.toString())
				.filter { it.direction == OrderDirection.ORDER_DIRECTION_SELL }
				.filter { it.instrumentUid == strategy.instrumentId.toString() }
				.map { it.initialSecurityPrice.toDecimal() }

			strategyRepository.findPendingBuyOperations(strategy.id)
				.forEach { operation ->
					val incrementPrice: BigDecimal = findSellPrice(operation, strategy)
					if (!currentOrders.contains(incrementPrice)) {
						orderInvestService.createSellOrder(strategy, operation.id, incrementPrice.toQuotation())
					} else logger.warn("[ЗАКАЗ.SELL] Текущий заказ уже создан ${incrementPrice}!")
				}
	}

	private fun findSellPrice(
		operation: BuyOperation,
		strategy: AccountStrategy
	): BigDecimal {
		val buyPrice = operation.buyOrder.initialSecurityPrice.toDecimal()
		val commission = buyPrice.times(commission)
		val profit = buyPrice.times(profit)
		val orderPrice = buyPrice.plus(commission).plus(profit)
		val minPriceIncrement = shareCache.get(strategy.instrumentId).minPriceIncrement.toDecimal()
		var incrementPrice: BigDecimal = buyPrice
		do {
			incrementPrice = incrementPrice.plus(minPriceIncrement)
		} while (incrementPrice < orderPrice)
		return incrementPrice
	}


}
