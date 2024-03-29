package tinkoff.tinkoffbotapi.events

import tinkoff.tinkoffbotapi.model.AccountStrategy

data class StrategyBuyEvent(
	val strategy: AccountStrategy
)
