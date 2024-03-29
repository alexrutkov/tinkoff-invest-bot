package tinkoff.tinkoffbotapi.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks
import ru.tinkoff.piapi.contract.v1.*
import ru.tinkoff.piapi.core.OperationsService
import ru.tinkoff.piapi.core.OrdersService
import ru.tinkoff.piapi.core.exception.ApiRuntimeException
import tinkoff.tinkoffbotapi.dto.CreateBuyOrderCommand
import tinkoff.tinkoffbotapi.dto.CreateLimitOrderCommand
import tinkoff.tinkoffbotapi.dto.CreateSellOrderCommand
import tinkoff.tinkoffbotapi.extensions.toDecimal
import tinkoff.tinkoffbotapi.model.AccountStrategy
import tinkoff.tinkoffbotapi.repositories.AccountOrderRepository
import tinkoff.tinkoffbotapi.repositories.OperationRepository
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.function.BiPredicate

const val RU_FIGI = "RUB000UTSTOM"
private val logger = KotlinLogging.logger {}
typealias OrderCache = Cache<Pair<UUID, BigDecimal>, CreateLimitOrderCommand>
@Service
class OrderInvestService(
	private val orderService: OrdersService,
	private val operationRepository: OperationRepository,
	private val shareCache: LoadingCache<UUID, Share>,
	private val operationsService: OperationsService,
	private val accountOrderRepository: AccountOrderRepository,
	@Value("\${app.account.id}")
	private val accountId: String
) {

	private val buySource = Sinks.unsafe().many().replay().limit<CreateBuyOrderCommand>(Duration.ofMinutes(3))
	private val sellSource = Sinks.unsafe().many().replay().limit<CreateSellOrderCommand>(Duration.ofMinutes(3))
	private final val buyCommands = buySource.asFlux()
	private final val sellCommands = sellSource.asFlux()

	private val cache: OrderCache = Caffeine.newBuilder()
		.expireAfterWrite(10, TimeUnit.MINUTES)
		.build()

	private val predicateFn = BiPredicate<OrderCache, CreateLimitOrderCommand> { c, value ->
		val key = Pair(value.strategy.instrumentId, value.price.toDecimal())
		if (c.getIfPresent(key) != null) {
			false
		} else {
			c.put(key, value)
			true
		}
	}

	init {
		buyCommands
			.distinct({ it }, { cache }, predicateFn, { cache.cleanUp() })
			.delayElements(Duration.ofSeconds(5))
			.doOnNext { logger.info("[ЗАКАЗ.BUY] ${it.price.toDecimal()}") }
			.subscribe(::createLimitOrder)

		sellCommands
			.distinct({ it }, { cache }, predicateFn, { cache.cleanUp() })
			.delayElements(Duration.ofSeconds(3))
			.doOnNext { logger.info("[ЗАКАЗ.SELL] ${it.price.toDecimal()}") }
			.subscribe { command ->
				val share = shareCache.get(command.strategy.instrumentId)
				val quantityShare = operationsService.getPositionsSync(command.strategy.accountId.toString()).securities
					.find { it.figi == share.figi }?.balance ?: 0
				if (quantityShare > 0) {
					try {
						createLimitOrder(command).also { operationRepository.saveSellOrder(command.operationId, it.orderId) }
					} catch (ex: ApiRuntimeException) {
						logger.error("[ОШИБКА] ${ex.message} для ${share.figi} и баланс $quantityShare")
					}

				}
			}
	}

	fun createOrder(strategy: AccountStrategy, price: Quotation) {
		buySource.tryEmitNext(CreateBuyOrderCommand(strategy, price, UUID.randomUUID()))
	}

	fun createSellOrder(strategy: AccountStrategy, operationId: Long, price: Quotation) {
		sellSource.tryEmitNext(CreateSellOrderCommand(strategy, price, UUID.randomUUID(), operationId))
	}

	fun cancelOrder(orderId: String) {
		orderService.cancelOrderSync(accountId, orderId)
		val orderState = orderService.getOrderStateSync(accountId, orderId)
		this.accountOrderRepository.saveOrderState(UUID.fromString(orderId), orderState)
		logger.info("[ORDER] Отменен заказ $orderId")
	}

	private fun createLimitOrder(command: CreateLimitOrderCommand): PostOrderResponse {
		return orderService.postLimitOrderSync(
			command.strategy.instrumentId.toString(),
			1, command.price, command.direction, accountId, TimeInForceType.TIME_IN_FORCE_DAY, command.id.toString()
		).also { order ->
			this.accountOrderRepository.saveStrategyOrder(
				command.strategy.id, order,
				orderService.getOrderStateSync(accountId, order.orderId)
			)
		}
	}
}
