package tinkoff.tinkoffbotapi.model

import java.util.UUID

data class AccountStrategy(
	val id: Long,
	val accountId: UUID,
	val instrumentId: UUID,
	val strategyType: InvestStrategyType
)
