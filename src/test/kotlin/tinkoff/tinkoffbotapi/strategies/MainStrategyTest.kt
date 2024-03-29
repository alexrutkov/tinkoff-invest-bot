package tinkoff.tinkoffbotapi.strategies

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.random.Random

class MainStrategyTest {


	@Test
	fun testChain() {
		val prices = arrayOfNulls<BigDecimal>(500).asList()
			.map { Random.nextInt(0, 1000).toBigDecimal() }.sortedDescending()
		println(findMinPrice(prices.first(), prices))
	}

	private fun findMinPrice(lastPrice: BigDecimal, all: List<BigDecimal>): List<BigDecimal> {
		val delta = lastPrice.times(BigDecimal.valueOf(0.01))
		val minPrice = lastPrice.minus(delta)
		return all.firstOrNull { minPrice > it }
			?.let { price -> listOf(price).plus(findMinPrice(price, all.filter { it < price })) }
			?: emptyList()
	}
}
