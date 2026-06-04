package life.util.data

import com.fasterxml.jackson.annotation.JsonPropertyDescription

data class Cmd(
    @get:JsonPropertyDescription("The bash command to execute.")
    val command: String,
)
