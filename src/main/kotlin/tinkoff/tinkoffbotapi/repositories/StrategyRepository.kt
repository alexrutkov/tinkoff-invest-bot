package tinkoff.tinkoffbotapi.repositories

import com.google.protobuf.util.JsonFormat
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import ru.tinkoff.piapi.contract.v1.OrderState
import tinkoff.tinkoffbotapi.model.AccountStrategy
import tinkoff.tinkoffbotapi.model.BuyOperation
import tinkoff.tinkoffbotapi.model.InvestStrategyType
import java.sql.ResultSet
import java.util.*


val mapToStrategy = RowMapper<AccountStrategy> { rs, _ ->
	AccountStrategy(
		rs.getLong("id"), UUID.fromString(rs.getString("account_id")),
		UUID.fromString(rs.getString("instrument_id")),
		InvestStrategyType.valueOf(rs.getString("type"))
	)
}
@Repository
class StrategyRepository(
	private val jdbcClient: JdbcClient,
	private val jsonToProtobufMapper: JsonFormat.Parser
) {
	fun findStrategyBy(accountId: String, type: InvestStrategyType): Optional<AccountStrategy> {
		return jdbcClient.sql("""
			select * from account_strategies 
			where account_id = :accountId and type = :type::strategy_type 
		""".trimIndent())
			.param("accountId", UUID.fromString(accountId))
			.param("type", type.name)
			.query(mapToStrategy).optional()
	}

	fun create(strategy: AccountStrategy) {
		val keyHolder = GeneratedKeyHolder()
		jdbcClient.sql("""
			insert into account_strategies (account_id, instrument_id, type, details) 
			values (:accountId, :instrumentId, :type::strategy_type, :details::jsonb)
			on conflict (account_id, instrument_id, type) do nothing 
			returning id
		""".trimIndent())
			.param("accountId", strategy.accountId)
			.param("instrumentId", strategy.instrumentId)
			.param("type", strategy.strategyType.name)
			.param("details", "{}")
			.update(keyHolder)
	}

	fun findPendingBuyOperations(id: Long): List<BuyOperation> {
		return jdbcClient.sql("""
			select o.id, o.buy_order_details from strategy_orders so
				join operations o on o.buy_order_details->>'orderId' = so.order_id::text
				where so.strategy_id = :strategyId and o.sell_order_details is null
		""".trimIndent())
			.param("strategyId", id)
			.query { rs, _ ->
				BuyOperation(
					rs.getLong("id"),
					mapToOrderState(rs.getString("buy_order_details"))
				)
			}.list()

	}

	private fun mapToOrderState(details: String): OrderState {
		val builder = OrderState.newBuilder()
		JsonFormat.parser().ignoringUnknownFields().merge(details, builder)
		return builder.build()
	}

	fun findActiveStrategies(): List<AccountStrategy> {
		return jdbcClient.sql("""
			select * from account_strategies
		""".trimIndent())
			.query(mapToStrategy)
			.list()
	}

	fun resetAllOperationSeller() {
		jdbcClient.sql("""
			delete from operation_sell_orders where order_id is not null
		""".trimIndent())
			.update()
	}

}
