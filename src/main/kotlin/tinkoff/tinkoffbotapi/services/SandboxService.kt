package tinkoff.tinkoffbotapi.services

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ru.tinkoff.piapi.contract.v1.MoneyValue
import ru.tinkoff.piapi.core.InvestApi


private val logger = KotlinLogging.logger {}
@Component
class SandboxService(
	private val sandboxApi: InvestApi,
	@Value("\${app.account.id}")
	private val accountId: String
) {

	@PostConstruct
	 fun run() {
//		sandboxApi.sandboxService.closeAccountSync(accountId)
//		createAccount()
//		addInitialMoney()

	}

	private fun addInitialMoney() {
		sandboxApi.sandboxService.payIn(accountId, MoneyValue.newBuilder().setUnits(1_000_000).setCurrency("RUB").build())
	}

	private fun createAccount() {
		val accountId = Mono.fromFuture(sandboxApi.sandboxService.openAccount("Разработка")).block()
		logger.info("открыт новый аккаунт в песочнице {}", accountId)
	}

}
