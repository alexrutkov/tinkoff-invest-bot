package tinkoff.tinkoffbotapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.tinkoff.piapi.core.*
import ru.tinkoff.piapi.core.stream.MarketDataStreamService
import ru.tinkoff.piapi.core.stream.OperationsStreamService
import ru.tinkoff.piapi.core.stream.OrdersStreamService

@Configuration
class TinkoffBotConfiguration {

//	@Profile("default")
	@Bean
	fun investApi(@Value("\${app.sandbox.token}") token: String): InvestApi = InvestApi.createSandbox(token)

	@Bean
	fun instrumentsService(api: InvestApi): InstrumentsService = api.instrumentsService

	@Bean
	fun ordersService(api: InvestApi): OrdersService = api.ordersService

	@Bean
	fun ordersStreamService(api: InvestApi): OrdersStreamService = api.ordersStreamService

	@Bean
	fun stopOrdersService(api: InvestApi): StopOrdersService = api.stopOrdersService

	@Bean
	fun userService(api: InvestApi): UsersService = api.userService

	@Bean
	fun marketDataService(api: InvestApi): MarketDataService = api.marketDataService

	@Bean
	fun marketDataStreamService(api: InvestApi): MarketDataStreamService = api.marketDataStreamService

	@Bean
	fun operationsService(api: InvestApi): OperationsService = api.operationsService

	@Bean
	fun operationsStreamService(api: InvestApi): OperationsStreamService? = api.operationsStreamService


}
