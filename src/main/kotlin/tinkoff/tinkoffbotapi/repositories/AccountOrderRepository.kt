package tinkoff.tinkoffbotapi.repositories

import com.google.protobuf.util.JsonFormat
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.tinkoff.piapi.contract.v1.OrderExecutionReportStatus
import ru.tinkoff.piapi.contract.v1.OrderState
import ru.tinkoff.piapi.contract.v1.PostOrderResponse
import tinkoff.tinkoffbotapi.model.AccountStrategy
import java.util.UUID

@Repository
class AccountOrderRepository(
	private val jdbcClient: JdbcClient,
	private val mapper: JsonFormat.Printer
) {
	private val pendingStatus = listOf(
		OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW
	)
	fun saveStrategyOrder(strategyId: Long, order: PostOrderResponse, state: OrderState) {
		jdbcClient.sql("""
			insert into strategy_orders (order_id, strategy_id, details, state) 
			values (:orderId, :strategyId, :details::jsonb, :state::jsonb)
			on conflict (order_id) do update set details = excluded.details::jsonb
		""".trimIndent())
			.param("strategyId", strategyId)
			.param("orderId", UUID.fromString(order.orderId))
			.param("details", mapper.print(order))
			.param("state", mapper.print(state))
			.update()
	}

	fun findPendingOrders(): List<UUID> {
		return jdbcClient.sql("""
			select order_id from strategy_orders 
			where state->>'executionReportStatus' in (:status)
		""".trimIndent())
			.param("status", pendingStatus.map(OrderExecutionReportStatus::name))
			.query {rs, _ -> UUID.fromString(rs.getString("order_id"))}
			.list()
	}

	fun saveOrderState(orderId: UUID, orderState: OrderState) {
		jdbcClient.sql("""
			update strategy_orders set state = :state::jsonb where order_id = :orderId
		""".trimIndent())
			.param("orderId", orderId)
			.param("state", mapper.print(orderState))
			.update()
	}

	fun findPendingOrdersByInstrument(instrumentId: UUID): List<OrderState> {
		return jdbcClient.sql("""
			select state from strategy_orders 
			where
			 state->>'instrumentUid' = :instrumentId and state->>'executionReportStatus' in (:status)
		""".trimIndent())
			.param("status", pendingStatus.map(OrderExecutionReportStatus::name))
			.param("instrumentId", instrumentId.toString())
			.query {rs, _ ->
				val builder = OrderState.newBuilder()
				JsonFormat.parser().ignoringUnknownFields().merge(rs.getString("state"), builder)
				builder.build()
			}
			.list()
	}

	fun getStrategyByOrderId(orderId: UUID): AccountStrategy {
		return jdbcClient.sql("""
			select 
				s.id, s.details, s.instrument_id, s.account_id, s.type
			from strategy_orders so
				join account_strategies s on so.strategy_id = s.id
			where order_id = :orderId
		""".trimIndent())
			.param("orderId", orderId)
			.query(mapToStrategy)
			.single()
	}

}
