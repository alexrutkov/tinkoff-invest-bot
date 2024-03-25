package tinkoff.tinkoffbotapi.sinks

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import ru.tinkoff.piapi.contract.v1.OrderTrades
import ru.tinkoff.piapi.core.InvestApi
import java.time.Duration
private val logger = KotlinLogging.logger {}
@Component
class TradesInvestSink(
	private val investApi: InvestApi,
	@Value("\${app.account.id}")
	private val accountId: String,
) {
	private val hotSource = Sinks.unsafe().many().replay().limit<OrderTrades>(Duration.ofSeconds(3))
	val trades = hotSource.asFlux()

	init {
		investApi.ordersStreamService.subscribeTrades { response ->
			if (response.hasOrderTrades()) hotSource.tryEmitNext(response.orderTrades)
		}
	}
}
