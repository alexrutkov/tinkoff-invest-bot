package tinkoff.tinkoffbotapi.services

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import ru.tinkoff.piapi.contract.v1.OrderDirection
import ru.tinkoff.piapi.contract.v1.OrderExecutionReportStatus
import ru.tinkoff.piapi.core.OrdersService
import tinkoff.tinkoffbotapi.events.StrategyBuyEvent
import tinkoff.tinkoffbotapi.extensions.toDecimal
import tinkoff.tinkoffbotapi.repositories.AccountOrderRepository
import tinkoff.tinkoffbotapi.repositories.OperationRepository
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.function.BiPredicate

private val logger = KotlinLogging.logger {}
@Component
class OrderTradingService(
	private val operationRepository: OperationRepository,
	private val ordersService: OrdersService,
	private val publisher: ApplicationEventPublisher,
	private val accountOrderRepository: AccountOrderRepository,
	@Value("\${app.account.id}")
	private val accountId: String
) {

	private val hotSource = Sinks.unsafe().many().replay().limit<UUID>(Duration.ofMinutes(1))
	private val orderIds = hotSource.asFlux()

	private val cache: Cache<UUID, Any> = Caffeine.newBuilder()
		.expireAfterWrite(2, TimeUnit.MINUTES)
		.build()

	private val predicateFn = BiPredicate<Cache<UUID, Any>, UUID> { c, value ->
		if (c.getIfPresent(value) != null) {
			false
		} else {
			c.put(value, value)
			true
		}
	}

	init {
		orderIds
			.distinct({ it }, { cache }, predicateFn, { cache.cleanUp() })
			.delayElements(Duration.ofSeconds(3))
			.subscribe(::updateOrderState)
	}

	private fun updateOrderState(orderId: UUID) {
		val orderState = ordersService.getOrderStateSync(accountId, orderId.toString())
		this.accountOrderRepository.saveOrderState(orderId, orderState)
		if (orderState.executionReportStatus == OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL) {
			when (orderState.direction) {
				OrderDirection.ORDER_DIRECTION_BUY -> {
					operationRepository.saveBuyOperation(orderState)
					val strategy = accountOrderRepository.getStrategyByOrderId(orderId)
					publisher.publishEvent(StrategyBuyEvent(strategy))
					logger.info("[BUY] ${orderState.initialSecurityPrice.toDecimal()}")
				}
				OrderDirection.ORDER_DIRECTION_SELL -> {
					logger.info("[SELL] ${orderState.initialSecurityPrice.toDecimal()}")
					val operationId = operationRepository.findOperationByOrderId(orderState.orderId)
					operationRepository.saveSellOperation(operationId, orderState)
				}

				else -> TODO()
			}
		}
	}

	fun refreshOrderStatesByInstruments(instruments: List<UUID>) {
		instruments.flatMap(this.accountOrderRepository::findPendingOrdersByInstrument)
			.map { it.orderId }.map(UUID::fromString)
			.forEach(hotSource::tryEmitNext)
	}


}
