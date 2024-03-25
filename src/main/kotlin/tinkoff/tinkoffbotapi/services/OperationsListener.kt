package tinkoff.tinkoffbotapi.services

import org.springframework.stereotype.Service
import ru.tinkoff.piapi.contract.v1.OperationType
import ru.tinkoff.piapi.core.OperationsService
import tinkoff.tinkoffbotapi.repositories.OperationRepository
import tinkoff.tinkoffbotapi.sinks.PositionsInvestSink
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class OperationsListener(
	positionSink: PositionsInvestSink,
	private val operationsService: OperationsService,
	private val operationRepository: OperationRepository

) {

	private val operationTypes = listOf(OperationType.OPERATION_TYPE_SELL, OperationType.OPERATION_TYPE_BUY)

	init {
		positionSink.positions.subscribe { position ->
			operationsService.getAllOperationsSync(
				position.accountId, Instant.now().minus(1, ChronoUnit.MINUTES), Instant.now()
			)
				.filter { operationTypes.contains(it.operationType) }
				.forEach { operationItem ->
					operationRepository.saveOperation(operationItem)
					operationItem.tradesList.forEach {
						operationRepository.saveTrade(operationItem.id, it)
					}
				}
		}
	}
}
