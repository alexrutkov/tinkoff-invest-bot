package tinkoff.tinkoffbotapi.controllers

import com.google.protobuf.util.JsonFormat
import org.springframework.beans.factory.annotation.Value
import org.springframework.util.MimeType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ru.tinkoff.piapi.contract.v1.Instrument
import ru.tinkoff.piapi.contract.v1.Share
import ru.tinkoff.piapi.core.InstrumentsService
import ru.tinkoff.piapi.core.OperationsService
import ru.tinkoff.piapi.core.OrdersService
import tinkoff.tinkoffbotapi.dto.AddInstrumentCommand
import tinkoff.tinkoffbotapi.dto.ProtoDetails
import tinkoff.tinkoffbotapi.model.AccountStrategy
import tinkoff.tinkoffbotapi.model.InvestStrategyType
import tinkoff.tinkoffbotapi.repositories.StrategyRepository
import tinkoff.tinkoffbotapi.services.OrderInvestService
import java.util.*

@RestController
class ProfileController(
	private val operationsService: OperationsService,
	private val ordersService: OrdersService,
	private val instrumentsService: InstrumentsService,
	private val orderInvestService: OrderInvestService,

	private val strategyRepository: StrategyRepository,
	private val mapper: JsonFormat.Printer,
	@Value("\${app.account.id}")
	private val accountId: String
) {

	@GetMapping("portfolio")
	fun getProfile() = operationsService.getPortfolioSync(accountId)

	@GetMapping("positions")
	fun getPositions() = operationsService.getPositionsSync(accountId)

	@PostMapping("addInstrument")
	fun addInstrument(
		@RequestBody command: AddInstrumentCommand
	) {
		instrumentsService.getShareByUidSync(command.instrumentId)
			.also {
				this.strategyRepository.create(
					AccountStrategy(0, UUID.fromString(accountId), UUID.fromString(it.uid), InvestStrategyType.SMA)
				)
			}
//			.map(mapper::print)
	}


	@DeleteMapping("cancelAllOrder")
	fun cancelAllOrder() {
		ordersService.getOrdersSync(accountId)
			.forEach { orderInvestService.cancelOrder(it.orderId) }
		strategyRepository.resetAllOperationSeller()
	}

	@GetMapping("instruments")
	fun getInstruments() = instrumentsService.allSharesSync
		.filter { it.currency == "rub" }
		.filter { it.apiTradeAvailableFlag }
		.filter { !it.forQualInvestorFlag }
		.map { ProtoDetails(mapper.print(it)) }
}
