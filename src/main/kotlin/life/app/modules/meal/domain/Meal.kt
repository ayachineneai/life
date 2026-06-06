package life.app.modules.meal.domain

import life.infra.schema.PropDesc
import java.time.LocalDateTime

data class Meal(
    @get:PropDesc("Short meal title, such as lunch at a restaurant.")
    val title: String,
    @get:PropDesc("Meal type. Use BREAKFAST, LUNCH, DINNER, or SNACK.")
    val mealType: MealType,
    @get:PropDesc("Food and drink content in plain text.")
    val content: String?,
    @get:PropDesc("Optional note about the meal.")
    val remark: String?,
    @get:PropDesc("Estimated total calories in kcal.")
    val calories: Int?,
    @get:PropDesc("Estimated protein in grams.")
    val protein: Int?,
    @get:PropDesc("Estimated fat in grams.")
    val fat: Int?,
    @get:PropDesc("Estimated carbohydrates in grams.")
    val carbs: Int?,
    @get:PropDesc("Local occurrence time. Use exactly yyyy-MM-dd'T'HH:mm:ss, for example 2026-06-05T12:00:00. Do not include timezone, Z, offset, milliseconds, or fractional seconds.")
    val occurredTime: LocalDateTime,
)
