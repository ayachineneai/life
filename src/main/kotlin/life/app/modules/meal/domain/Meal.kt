package life.app.modules.meal.domain

import java.time.LocalDateTime

data class Meal(
    val title: String,
    val mealType: MealType,
    val content: String?,
    val remark: String?,
    val calories: Int?,
    val protein: Int?,
    val fat: Int?,
    val carbs: Int?,
    val occurredTime: LocalDateTime,
)
