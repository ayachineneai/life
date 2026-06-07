package life.app.modules.meal

import life.app.modules.meal.domain.MealType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.uuid.Uuid

data class MealPo(
    val id: Uuid,
    val title: String? = null,
    val mealType: MealType? = null,
    val content: String? = null,
    val remark: String? = null,
    val calories: Int? = null,
    val protein: Int? = null,
    val fat: Int? = null,
    val carbs: Int? = null,
    val occurredDate: LocalDate? = null,
    val occurredTime: LocalDateTime? = null,
    val createTime: LocalDateTime,
    val updateTime: LocalDateTime? = null,
)

object MealTable : Table("meal") {
    val id = uuid("id")
    val title = varchar("title", 128).nullable()
    val mealType = enumerationByName<MealType>("meal_type", 16).nullable()
    val content = text("content").nullable()
    val remark = text("remark").nullable()
    val calories = integer("calories").nullable()
    val protein = integer("protein").nullable()
    val fat = integer("fat").nullable()
    val carbs = integer("carbs").nullable()
    val occurredDate = date("occurred_date").nullable()
    val occurredTime = datetime("occurred_time").nullable()
    val createTime = datetime("create_time")
    val updateTime = datetime("update_time").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(
            customIndexName = "idx_meal_occurred_date_meal_type_not_snack",
            columns = arrayOf(occurredDate, mealType),
            filterCondition = { mealType neq MealType.SNACK },
        )
    }
}
