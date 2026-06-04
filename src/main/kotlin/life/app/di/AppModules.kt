package life.app.di

import life.app.ai.conversation.ConversationService
import life.app.modules.meal.MealService
import org.koin.dsl.module

val appModule = module {
    single { ConversationService() }
    single { MealService() }
}
