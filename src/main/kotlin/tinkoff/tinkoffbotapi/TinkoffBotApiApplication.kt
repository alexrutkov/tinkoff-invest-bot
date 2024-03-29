package tinkoff.tinkoffbotapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class TinkoffBotApiApplication

fun main(args: Array<String>) {
	runApplication<TinkoffBotApiApplication>(*args)
}
