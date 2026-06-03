package life.util

import com.github.f4b6a3.uuid.UuidCreator
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

object Uuids {
    fun uuid7(): Uuid {
        return UuidCreator.getTimeOrderedEpoch().toKotlinUuid()
    }
}
