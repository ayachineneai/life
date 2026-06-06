package life.app.modules.meal

import life.app.modules.meal.domain.Meal
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder

object MealMapper {
    fun apply(mealPo: MealPo, meal: Meal): MealPo {
        return mealPo.copy(
            title = meal.title,
            mealType = meal.mealType,
            content = meal.content,
            remark = meal.remark,
            calories = meal.calories,
            protein = meal.protein,
            fat = meal.fat,
            carbs = meal.carbs,
            occurredDate = meal.occurredTime.toLocalDate(),
            occurredTime = meal.occurredTime,
        )
    }

    fun fillInsert(row: UpdateBuilder<*>, meal: MealPo) {
        fillMeal(row, meal)
        row[MealTable.id] = meal.id
        row[MealTable.createTime] = meal.createTime
    }

    fun fillUpdate(row: UpdateBuilder<*>, meal: MealPo) {
        fillMeal(row, meal)
        row[MealTable.updateTime] = meal.updateTime
    }

    private fun fillMeal(row: UpdateBuilder<*>, meal: MealPo) {
        row[MealTable.title] = meal.title
        row[MealTable.mealType] = meal.mealType
        row[MealTable.content] = meal.content
        row[MealTable.remark] = meal.remark
        row[MealTable.calories] = meal.calories
        row[MealTable.protein] = meal.protein
        row[MealTable.fat] = meal.fat
        row[MealTable.carbs] = meal.carbs
        row[MealTable.occurredDate] = meal.occurredDate
        row[MealTable.occurredTime] = meal.occurredTime
    }

    fun toPo(row: ResultRow): MealPo {
        return MealPo(
            id = row[MealTable.id],
            title = row[MealTable.title],
            mealType = row[MealTable.mealType],
            content = row[MealTable.content],
            remark = row[MealTable.remark],
            calories = row[MealTable.calories],
            protein = row[MealTable.protein],
            fat = row[MealTable.fat],
            carbs = row[MealTable.carbs],
            occurredDate = row[MealTable.occurredDate],
            occurredTime = row[MealTable.occurredTime],
            createTime = row[MealTable.createTime],
            updateTime = row[MealTable.updateTime],
        )
    }
}
