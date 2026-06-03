package life.infra.db

import io.github.cdimascio.dotenv.Dotenv
import life.util.required

data class DbConfig(
    val url: String,
    val username: String,
    val password: String,
) {
    companion object {
        fun fromEnv(dotenv: Dotenv): DbConfig {
            return DbConfig(
                url = dotenv.required("DB_URL"),
                username = dotenv.required("DB_USERNAME"),
                password = dotenv.required("DB_PASSWORD"),
            )
        }
    }
}
