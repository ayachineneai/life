package life.app.modules.meal

import life.app.ai.tool.annotations.Tool
import life.app.modules.meal.domain.Meal
import life.app.modules.meal.domain.MealType
import life.util.Times
import life.util.Uuids
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime
import kotlin.uuid.Uuid

class MealService(
    private val mealDao: MealDao
) {
    fun listByOccurredTimeRange(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ): List<MealPo> {
        return transaction {
            mealDao.listByOccurredTimeRange(startTime, endTime)
        }
    }

    fun deleteById(id: Uuid): Boolean {
        return transaction {
            mealDao.deleteById(id)
        }
    }

    @Tool(
        name = "record_meal",
        description = "Records or updates one meal entry for a specific meal type and local occurrence time.",
    )
    fun recordMeal(meal: Meal): MealPo {
        val now = Times.now()
        val mealDate = meal.occurredTime.toLocalDate()
        val newMeal = MealMapper.apply(
            mealPo = MealPo(
                id = Uuids.uuid7(),
                createTime = now,
            ),
            meal = meal,
        )

        return transaction {
            if (meal.mealType == MealType.SNACK) {
                mealDao.insert(newMeal)
                return@transaction newMeal
            }

            val existingMeal = mealDao.findNonSnackByDateAndMealType(mealDate, meal.mealType)

            if (existingMeal != null) {
                val updated = MealMapper.apply(existingMeal, meal).copy(updateTime = now)
                mealDao.update(updated)
                return@transaction updated
            }

            mealDao.insert(newMeal)
            newMeal
        }
    }

}
