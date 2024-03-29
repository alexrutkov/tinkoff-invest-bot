package tinkoff.tinkoffbotapi.services

import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import java.math.BigDecimal
import java.time.Duration
import kotlin.random.Random

class OrderTradingServiceTest {



	fun fluxOrderTimeout() {
		val prices = arrayOfNulls<BigDecimal>(500).asList()
			.map { Random.nextInt(0, 1000).toBigDecimal() }.sortedDescending()
		val cache = mutableSetOf<BigDecimal>()
		Flux.fromIterable(prices)
			.delayElements(Duration.ofSeconds(1))
			.distinct(
				{ it },
				{
					println("Вытаскиваю коллекцию")
					cache
				},
				{ c, value ->
					println("Проверяю коллекцию")
					if (c.contains(value)) {
						false
					} else {
						c.add(value)
						true
					}
				},
				{
					println("Очищаю коллекцию")
					cache.clear()
				}
			)
			.doOnNext { println(it) }
			.buffer(Duration.ofMinutes(1))
			.blockFirst()
	}
}
