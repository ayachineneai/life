package life.util

import java.time.LocalDateTime
import java.time.ZoneId.of

object Times {
    private const val ASIA_SHANGHAI = "Asia/Shanghai"

    fun now(): LocalDateTime {
        return LocalDateTime.now(of(ASIA_SHANGHAI))
    }
}
