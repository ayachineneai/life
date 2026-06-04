package life.infra.db

import io.github.cdimascio.dotenv.Dotenv
import life.util.required

data class DbConfig(
    val url: String,
    val username: String,
    val password: String,
)
