package tinkoff.tinkoffbotapi.config

import com.google.protobuf.util.JsonFormat
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SerializationConfig {

	@Bean
	fun protobufToJsonMapper(): JsonFormat.Printer = JsonFormat.printer()
}
