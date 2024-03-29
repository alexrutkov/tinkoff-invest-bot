package tinkoff.tinkoffbotapi.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AddInstrumentCommand(
	@JsonProperty("instrumentId")
	val instrumentId: String
)
