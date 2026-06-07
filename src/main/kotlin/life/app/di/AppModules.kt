package life.app.di

import life.app.ai.conversation.ConversationDao
import life.app.ai.conversation.ConversationService
import life.app.modules.meal.MealDao
import life.app.modules.meal.MealService
import org.koin.dsl.module

val appModule = module {
    single { ConversationDao() }
    single { ConversationService(get(), get()) }
    single { MealDao() }
    single { MealService(get()) }
}
