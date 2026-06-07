package life.util

import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

object Uuids {
    fun uuid7(): Uuid {
        return UuidCreator.getTimeOrderedEpoch().toKotlinUuid()
    }

    fun parse(value: String): Uuid {
        return UUID.fromString(value).toKotlinUuid()
    }
}
