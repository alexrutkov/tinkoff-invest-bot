package tinkoff.tinkoffbotapi.schedulers

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import tinkoff.tinkoffbotapi.repositories.StrategyRepository
import tinkoff.tinkoffbotapi.strategies.MainStrategy

private val logger = KotlinLogging.logger {}

@Component
class StrategyScheduler(
	private val strategyRepository: StrategyRepository,
	private val strategy: MainStrategy
) {

	@PostConstruct
	@Scheduled(cron = "0 0/3 * * * *")
	fun runActiveStrategies() {
		strategyRepository.findActiveStrategies()
			.forEach(this.strategy::addStrategy)
	}
}
