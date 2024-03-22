package tinkoff.tinkoffbotapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TinkoffBotApiApplication

fun main(args: Array<String>) {
	runApplication<TinkoffBotApiApplication>(*args)
}
