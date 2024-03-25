package tinkoff.tinkoffbotapi.sinks

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import ru.tinkoff.piapi.contract.v1.PositionData
import ru.tinkoff.piapi.core.InvestApi
import java.time.Duration

private val logger = KotlinLogging.logger {}
@Component
class PositionsInvestSink(
	investApi: InvestApi,
	@Value("\${app.account.id}")
	private val accountId: String,
) {
	private val hotSource = Sinks.unsafe().many().replay().limit<PositionData>(Duration.ofSeconds(3))
	val positions = hotSource.asFlux()
	init {
		investApi.operationsStreamService.subscribePositions(
			{ response -> if (response.hasPosition()) hotSource.tryEmitNext(response.position) },
			accountId
		)
	}
}
