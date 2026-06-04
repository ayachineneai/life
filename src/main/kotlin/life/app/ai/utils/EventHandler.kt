package life.app.ai.utils

import com.openai.models.responses.ResponseStreamEvent

typealias AiStreamEventHandler = (ResponseStreamEvent) -> Unit

object AiStreamEventHandlers {

    fun text(
        onText: (String) -> Unit,
        onError: (String) -> Unit = {},
    ): AiStreamEventHandler {
        return { event ->
            event.outputTextDelta().ifPresent { delta -> onText(delta.delta()) }
            event.refusalDelta().ifPresent { delta -> onText(delta.delta()) }
            event.error().ifPresent { error -> onError(error.message()) }
        }
    }
}
