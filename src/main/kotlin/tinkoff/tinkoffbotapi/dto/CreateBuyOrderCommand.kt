package tinkoff.tinkoffbotapi.dto

import ru.tinkoff.piapi.contract.v1.OrderDirection
import ru.tinkoff.piapi.contract.v1.Quotation
import tinkoff.tinkoffbotapi.model.AccountStrategy
import java.util.UUID

open class CreateLimitOrderCommand(
	open val strategy: AccountStrategy,
	open val price: Quotation,
	open val id: UUID,
	val direction: OrderDirection
)

data class CreateBuyOrderCommand(
	override val strategy: AccountStrategy,
	override val price: Quotation,
	override val id: UUID,
) : CreateLimitOrderCommand(strategy, price, id, OrderDirection.ORDER_DIRECTION_BUY)

data class CreateSellOrderCommand(
	override val strategy: AccountStrategy,
	override val price: Quotation,
	override val id: UUID,
	val operationId: Long
) : CreateLimitOrderCommand(strategy, price, id, OrderDirection.ORDER_DIRECTION_SELL)
