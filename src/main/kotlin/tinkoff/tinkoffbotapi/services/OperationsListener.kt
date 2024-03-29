package tinkoff.tinkoffbotapi.services

import mu.KotlinLogging
import org.springframework.stereotype.Service
import ru.tinkoff.piapi.contract.v1.OperationType
import tinkoff.tinkoffbotapi.sinks.PositionsInvestSink
import java.time.Duration
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
class OperationsListener(
	positionSink: PositionsInvestSink,
	private val tradingService: OrderTradingService

) {

	private val operationTypes = listOf(OperationType.OPERATION_TYPE_SELL, OperationType.OPERATION_TYPE_BUY)

	init {

		positionSink.positions
			.buffer(Duration.ofMinutes(1))
			.subscribe {
				val instruments = it.flatMap { it.securitiesList.map { it.instrumentUid } }.distinct()
					.map(UUID::fromString)
			logger.info("[ПОРТФЕЛЬ] $instruments")
			this.tradingService.refreshOrderStatesByInstruments(instruments)
		}
	}

}
