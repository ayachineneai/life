package life.app.modules.meal

import life.app.modules.meal.domain.Meal
import life.util.Times
import life.util.Uuids
import java.time.LocalDateTime
import kotlin.uuid.Uuid

class MealService(
    private val mealDao: MealDao = MealDao(),
) {
    fun listByOccurredTimeRange(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ): List<MealPo> {
        return mealDao.listByOccurredTimeRange(startTime, endTime)
    }

    fun deleteById(id: Uuid): Boolean {
        return mealDao.deleteById(id)
    }

    fun recordMeal(meal: Meal): MealPo {
        val now = Times.now()
        val mealDate = meal.occurredTime.toLocalDate()
        val existingMeal = mealDao.findByDateAndMealType(mealDate, meal.mealType)

        if (existingMeal != null) {
            val updated = MealMapper.apply(existingMeal, meal).copy(updateTime = now)
            mealDao.update(updated)
            return updated
        }

        val new = MealMapper.apply(
            mealPo = MealPo(
                id = Uuids.uuid7(),
                createTime = now,
            ),
            meal = meal,
        )
        mealDao.insert(new)
        return new
    }

}
