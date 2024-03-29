package tinkoff.tinkoffbotapi.repositories

import com.google.protobuf.util.JsonFormat
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.tinkoff.piapi.contract.v1.OrderState
import java.util.*

@Repository
class OperationRepository(
	private val jdbcClient: JdbcClient,
	private val mapper: JsonFormat.Printer
) {



	fun saveBuyOperation(orderState: OrderState) {
		jdbcClient.sql("""
			insert into operations (buy_order_details) 
			values (:details::jsonb)
		""".trimIndent())
			.param("details", mapper.print(orderState))
			.update()
	}

	fun saveSellOperation(operationId: Long, orderState: OrderState) {
		jdbcClient.sql("""
			update operations set sell_order_details = :details::jsonb where id = :id
		""".trimIndent())
			.param("details", mapper.print(orderState))
			.param("id", operationId)
			.update()
	}

	fun findOperationByOrderId(orderId: String): Long {
		return jdbcClient.sql("""
			select operation_id from operation_sell_orders where order_id = :orderId
		""".trimIndent())
			.param("orderId", UUID.fromString(orderId))
			.query(Long::class.java).single()
	}

	fun saveSellOrder(id: Long, orderId: String) {
		jdbcClient.sql("""
			insert into operation_sell_orders (operation_id, order_id) 
			VALUES (:id, :orderId)
		""".trimIndent())
			.param("id", id)
			.param("orderId", UUID.fromString(orderId))
			.update()
	}
}
