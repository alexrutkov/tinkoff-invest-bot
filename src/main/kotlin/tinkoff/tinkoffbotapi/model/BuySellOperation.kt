package tinkoff.tinkoffbotapi.model

import ru.tinkoff.piapi.contract.v1.OrderState

data class BuyOperation(
	val id: Long,
	val buyOrder: OrderState
)
