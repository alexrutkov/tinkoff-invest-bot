package tinkoff.tinkoffbotapi.repositories

import com.google.protobuf.util.JsonFormat
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.tinkoff.piapi.contract.v1.PostOrderResponse
import java.util.UUID

@Repository
class AccountOrderRepository(
	private val jdbcClient: JdbcClient,
	private val mapper: JsonFormat.Printer
) {
	fun saveStrategyOrder(strategyId: Long, order: PostOrderResponse) {
		jdbcClient.sql("""
			insert into strategy_orders (order_id, strategy_id, details) 
			values (:orderId, :strategyId, :details::jsonb)
			on conflict (order_id) do update set details = excluded.details::jsonb
		""".trimIndent())
			.param("strategyId", strategyId)
			.param("orderId", UUID.fromString(order.orderId))
			.param("details", mapper.print(order))
			.update()
	}

}
