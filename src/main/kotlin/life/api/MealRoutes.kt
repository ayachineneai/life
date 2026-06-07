package life.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import life.app.modules.meal.MealPo
import life.app.modules.meal.MealService
import life.app.modules.meal.domain.Meal
import life.app.modules.meal.domain.MealType
import life.util.Times
import java.time.LocalDateTime

fun Application.mealRoutes(
    mealService: MealService,
    mapper: ObjectMapper,
) {
    routing {
        get("/meals/today") {
            val today = Times.now().toLocalDate()
            val response = MealListResponse(
                meals = mealService.listByOccurredTimeRange(
                    startTime = today.atStartOfDay(),
                    endTime = today.plusDays(1).atStartOfDay(),
                ).map { meal -> meal.toResponse() },
            )
            call.respondText(
                text = mapper.writeValueAsString(response),
                contentType = ContentType.Application.Json,
            )
        }

        post("/meals") {
            val request = mapper.readValue(call.receiveText(), CreateMealRequest::class.java)
            val meal = mealService.recordMeal(
                Meal(
                    title = request.title,
                    mealType = request.mealType,
                    content = request.content,
                    remark = request.remark,
                    calories = request.calories,
                    protein = request.protein,
                    fat = request.fat,
                    carbs = request.carbs,
                    occurredTime = request.occurredTime,
                ),
            )
            call.respondText(
                text = mapper.writeValueAsString(meal.toResponse()),
                contentType = ContentType.Application.Json,
            )
        }
    }
}

data class MealListResponse(
    val meals: List<MealResponse>,
)

data class MealResponse(
    val id: String,
    val title: String?,
    val mealType: MealType?,
    val content: String?,
    val remark: String?,
    val calories: Int?,
    val protein: Int?,
    val fat: Int?,
    val carbs: Int?,
    val occurredTime: String?,
)

data class CreateMealRequest(
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

private fun MealPo.toResponse(): MealResponse {
    return MealResponse(
        id = id.toString(),
        title = title,
        mealType = mealType,
        content = content,
        remark = remark,
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs,
        occurredTime = occurredTime?.toString(),
    )
}
