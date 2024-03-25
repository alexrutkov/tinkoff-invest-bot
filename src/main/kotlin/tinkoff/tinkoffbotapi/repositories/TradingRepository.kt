package tinkoff.tinkoffbotapi.repositories

import com.google.protobuf.util.JsonFormat
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.tinkoff.piapi.contract.v1.OrderTrade
import java.util.UUID

@Repository
class TradingRepository(
	private val jdbcClient: JdbcClient,
	private val mapper: JsonFormat.Printer
) {
	fun saveTrade(orderId: String, trade: OrderTrade) {
		jdbcClient.sql("""
			insert into trade_orders (trade_id, order_id, details) 
			values (:tradeId, :orderId, :details::jsonb)
		""".trimIndent())
			.param("orderId", UUID.fromString(orderId))
			.param("tradeId", UUID.fromString(trade.tradeId))
			.param("details", mapper.print(trade))
			.update()
	}

}
