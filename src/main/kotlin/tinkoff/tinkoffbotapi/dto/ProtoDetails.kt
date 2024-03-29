package tinkoff.tinkoffbotapi.dto

import com.fasterxml.jackson.annotation.JsonRawValue

data class ProtoDetails(
	@JsonRawValue
	val details: String
)
