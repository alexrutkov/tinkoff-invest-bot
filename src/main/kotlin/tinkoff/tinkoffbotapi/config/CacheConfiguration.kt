package tinkoff.tinkoffbotapi.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.tinkoff.piapi.contract.v1.OrderState
import ru.tinkoff.piapi.contract.v1.Share
import ru.tinkoff.piapi.core.InstrumentsService
import ru.tinkoff.piapi.core.OrdersService
import tinkoff.tinkoffbotapi.repositories.AccountOrderRepository
import java.util.UUID
import java.util.concurrent.TimeUnit

@Configuration
class CacheConfiguration {

	@Bean
	fun shareCache(
		instrumentsService: InstrumentsService
	): LoadingCache<UUID, Share> = Caffeine.newBuilder()
		.expireAfterWrite(1, TimeUnit.DAYS)
		.build { instrumentsService.getShareByUidSync(it.toString()) }

/*	@Bean
	fun orderCache(
		ordersService: OrdersService
	): LoadingCache<UUID, Share> = Caffeine.newBuilder()
		.expireAfterWrite(1, TimeUnit.DAYS)
		.build { instrumentsService.getShareByUidSync(it.toString()) }*/


	@Bean
	fun pendingOrderCache(
		accountOrderRepository: AccountOrderRepository,
	): Cache<UUID, List<OrderState>> = Caffeine.newBuilder()
		.expireAfterWrite(1, TimeUnit.DAYS)
		.build { accountOrderRepository.findPendingOrdersByInstrument(it) }
}
