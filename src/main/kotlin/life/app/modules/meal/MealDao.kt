package life.app.modules.meal

import life.app.modules.meal.domain.MealType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.uuid.Uuid

class MealDao {
    fun insert(mealPo: MealPo): MealPo {
        MealTable.insert { row ->
            MealMapper.fillInsert(row, mealPo)
        }

        return mealPo
    }

    fun listAll(): List<MealPo> {
        return MealTable
            .selectAll()
            .orderBy(MealTable.occurredTime to SortOrder.DESC)
            .map { row -> MealMapper.toPo(row) }
    }

    fun listByOccurredTimeRange(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ): List<MealPo> {
        return MealTable
            .selectAll()
            .where { occurredTimeInRange(startTime, endTime) }
            .orderBy(MealTable.occurredTime to SortOrder.DESC)
            .map { row -> MealMapper.toPo(row) }
    }

    fun findByDateAndMealType(date: LocalDate, mealType: MealType): MealPo? {
        val startAt = date.atStartOfDay()
        val endAt = date.plusDays(1).atStartOfDay()

        return MealTable
            .selectAll()
            .where {
                occurredTimeInRange(startAt, endAt) and
                    (MealTable.mealType eq mealType)
            }
            .orderBy(MealTable.occurredTime to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.let { row -> MealMapper.toPo(row) }
    }

    fun update(mealPo: MealPo): Boolean {
        val affectedRows = MealTable.update({ MealTable.id eq mealPo.id }) { row ->
            MealMapper.fillUpdate(row, mealPo)
        }

        return affectedRows > 0
    }

    fun deleteById(id: Uuid): Boolean {
        return MealTable.deleteWhere { MealTable.id eq id } > 0
    }

    private fun occurredTimeInRange(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ): Op<Boolean> {
        return (MealTable.occurredTime greaterEq startTime) and
            (MealTable.occurredTime less endTime)
    }
}
