package tinkoff.tinkoffbotapi.services

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.tinkoff.piapi.core.InstrumentsService
import ru.tinkoff.piapi.core.MarketDataService
import tinkoff.tinkoffbotapi.extensions.toDecimal
import tinkoff.tinkoffbotapi.model.AccountStrategy
import tinkoff.tinkoffbotapi.model.InvestStrategyType
import tinkoff.tinkoffbotapi.repositories.StrategyRepository
import java.math.BigDecimal
import java.util.*


@Component
class StrategyService(
	private val instrumentsService: InstrumentsService,
	private val marketDataService: MarketDataService,
	private val strategyRepository: StrategyRepository,
	@Value("\${app.account.id}")
	private val accountId: String
) {

	private val MAX_SHARE_PRICE = BigDecimal.valueOf(10_000)
	@PostConstruct
	fun loadAvailableStrategies() {
		val allShares = instrumentsService.allSharesSync
			.filter { it.currency == "rub" }
			.filter { it.apiTradeAvailableFlag }
			.filter { !it.forQualInvestorFlag }
		val lastPrices = marketDataService.getLastPricesSync(allShares.map { it.uid })
		allShares
			.filter { share ->
				val lastPrice = lastPrices.find { share.uid == it.instrumentUid }?.price?.toDecimal() ?: BigDecimal.ZERO
				lastPrice != BigDecimal.ZERO &&
				lastPrice.times(share.lot.toBigDecimal()) < MAX_SHARE_PRICE
			}
			.forEach {
				this.strategyRepository.create(
					AccountStrategy(0, UUID.fromString(accountId), UUID.fromString(it.uid), InvestStrategyType.SMA)
				)
			}
	}
}
