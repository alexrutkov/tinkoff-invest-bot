package tinkoff.tinkoffbotapi.services

import org.springframework.stereotype.Component
import tinkoff.tinkoffbotapi.repositories.TradingRepository
import tinkoff.tinkoffbotapi.sinks.TradesInvestSink

@Component
class TradingListener(
	tradingSink: TradesInvestSink,
	private val tradingRepository: TradingRepository
) {

	init {
		tradingSink.trades.subscribe { orderTrades ->
			orderTrades.tradesList.forEach {
				tradingRepository.saveTrade(orderTrades.orderId, it)
			}
		}
	}

}
