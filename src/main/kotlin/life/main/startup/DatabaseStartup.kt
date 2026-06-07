package life.main.startup

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.Dotenv
import life.infra.db.DbConfig
import life.util.required
import org.jetbrains.exposed.v1.jdbc.Database

object DatabaseStartup {
    fun start(dotenv: Dotenv): Database {
        val config = config(dotenv)
        return start(config)
    }

    fun start(config: DbConfig): Database {
        return Database.connect(connectionPool(config))
    }

    fun config(dotenv: Dotenv): DbConfig {
        return DbConfig(
            url = dotenv.required("DB_URL"),
            username = dotenv.required("DB_USERNAME"),
            password = dotenv.required("DB_PASSWORD"),
        )
    }

    private fun connectionPool(config: DbConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            driverClassName = "org.postgresql.Driver"
            username = config.username
            password = config.password
        }
        return HikariDataSource(hikariConfig)
    }
}
