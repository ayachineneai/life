package life.util.data

import life.infra.schema.PropDesc

data class Cmd(
    @get:PropDesc("The bash command to execute.")
    val command: String,
)
