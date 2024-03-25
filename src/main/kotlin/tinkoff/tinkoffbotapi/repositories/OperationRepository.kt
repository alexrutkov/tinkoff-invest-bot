package tinkoff.tinkoffbotapi.repositories

import com.google.protobuf.util.JsonFormat
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.tinkoff.piapi.contract.v1.Operation
import ru.tinkoff.piapi.contract.v1.OperationTrade
import java.util.UUID

@Repository
class OperationRepository(
	private val jdbcClient: JdbcClient,
	private val mapper: JsonFormat.Printer
) {
	fun saveOperation(operation: Operation) {
		jdbcClient.sql("""
			insert into operations (id, details) 
			values (:id, :details::jsonb)
			on conflict (id) do update set details = excluded.details::jsonb
		""".trimIndent())
			.param("id", UUID.fromString(operation.id))
			.param("details", mapper.print(operation))
			.update()
	}

	fun saveTrade(operationId: String, trade: OperationTrade) {
		jdbcClient.sql("""
			insert into operation_trades (operation_id, trade_id, details) 
			values (:operationId, :tradeId, :details::jsonb)
			on conflict (operation_id, trade_id) do update set details = excluded.details::jsonb
		""".trimIndent())
			.param("operationId", UUID.fromString(operationId))
			.param("tradeId", UUID.fromString(trade.tradeId))
			.param("details", mapper.print(trade))
			.update()
	}
}
