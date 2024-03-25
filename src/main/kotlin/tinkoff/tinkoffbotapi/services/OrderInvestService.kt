package tinkoff.tinkoffbotapi.services

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.tinkoff.piapi.contract.v1.OrderDirection
import ru.tinkoff.piapi.contract.v1.OrderType
import ru.tinkoff.piapi.contract.v1.Quotation
import ru.tinkoff.piapi.core.InstrumentsService
import ru.tinkoff.piapi.core.MarketDataService
import ru.tinkoff.piapi.core.OperationsService
import ru.tinkoff.piapi.core.OrdersService
import ru.tinkoff.piapi.core.UsersService
import tinkoff.tinkoffbotapi.model.UID
import tinkoff.tinkoffbotapi.repositories.AccountOrderRepository
import java.util.UUID

const val RU_FIGI = "RUB000UTSTOM"
private val logger = KotlinLogging.logger {}
@Service
class OrderInvestService(
	private val marketDataService: MarketDataService,
	private val instrumentsService: InstrumentsService,
	private val orderService: OrdersService,
	private val usersService: UsersService,
	private val operationsService: OperationsService,
	private val accountOrderRepository: AccountOrderRepository,
	@Value("\${app.account.id}")
	private val accountId: String
) {

	private val strategyId = 1L
	fun createOrder(uid: UID) {
		val instrumentStatus = marketDataService.getTradingStatusSync(uid)
		val lastPrice = marketDataService.getLastPricesSync(listOf(uid)).first()
		val minPriceIncrement = instrumentsService.getShareByUidSync(uid).minPriceIncrement
		val amountMoney = operationsService.getPortfolioSync(accountId).positions.find { it.figi == RU_FIGI }!!.quantity
		val id = UUID.randomUUID().toString()
		val response = orderService.postOrderSync(
			uid,
			1,
			Quotation.getDefaultInstance(),
			OrderDirection.ORDER_DIRECTION_BUY,
			accountId,
			OrderType.ORDER_TYPE_BESTPRICE,
			id
			)

		this.accountOrderRepository.saveStrategyOrder(strategyId, response)
	}
}
