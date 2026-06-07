package life.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import life.app.ai.conversation.ConversationService
import life.app.ai.mainloop.support.MainLoopFactory
import life.infra.sse.SseWriter
import life.util.Uuids
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("life.api.ChatRoutes")

fun Application.chatRoutes(
    mainLoopFactory: MainLoopFactory,
    conversationService: ConversationService,
    mapper: ObjectMapper,
) {
    routing {
        get("/chat/conversations") {
            val limit = call.request.queryParameters["limit"]
                ?.toIntOrNull()
                ?.coerceIn(1, 50)
                ?: 10
            val turnLimit = call.request.queryParameters["turnLimit"]
                ?.toIntOrNull()
                ?.coerceIn(1, 50)
                ?: 10
            val response = ChatConversationsResponse(
                conversations = conversationService.listConversations(limit).map { conversation ->
                    ChatConversationResponse(
                        id = conversation.id.toString(),
                        title = conversation.title,
                        createTime = conversation.createTime.toString(),
                        lastActiveTime = conversation.lastActiveTime.toString(),
                        turns = conversationService.listTurns(
                            conversationId = conversation.id,
                            limit = turnLimit,
                        ).map { turn ->
                            ChatTurnResponse(
                                id = turn.id.toString(),
                                userInput = turn.userInput,
                                modelOutput = turn.modelOutput,
                                createTime = turn.createTime.toString(),
                            )
                        },
                    )
                },
            )

            call.respondText(
                text = mapper.writeValueAsString(response),
                contentType = ContentType.Application.Json,
            )
        }

        post("/chat/stream") {
            val requestId = UUID.randomUUID().toString()
            logger.info("[{}] POST /chat/stream received", requestId)
            val request = runCatching { readChatStreamRequest(mapper) }
                .getOrElse { error ->
                    logger.warn("[{}] Invalid chat stream request: {}", requestId, error.message)
                    call.respondText(
                        text = error.message ?: "Invalid chat stream request",
                        status = HttpStatusCode.BadRequest,
                    )
                    return@post
                }
            logger.info(
                "[{}] Chat stream start conversationId={} messageLength={} preview={}",
                requestId,
                request.conversationId ?: "new",
                request.message.length,
                request.message.preview(),
            )
            call.response.header(HttpHeaders.CacheControl, "no-cache")
            call.response.header("X-Accel-Buffering", "no")
            call.respondOutputStream(
                contentType = ContentType.Text.EventStream,
                status = HttpStatusCode.OK,
            ) {
                val output = this
                withContext(Dispatchers.IO) {
                    SseWriter(output, mapper).use { writer ->
                        val result = mainLoopFactory.create(writer).mainLoop(
                            conversationId = request.conversationId
                                ?.takeIf { it.isNotBlank() }
                                ?.let { id -> Uuids.parse(id) },
                            message = request.message,
                        )
                        logger.info(
                            "[{}] Chat stream done conversationId={} responseId={} outputLength={}",
                            requestId,
                            request.conversationId ?: "new",
                            result.responseId,
                            result.text.length,
                        )
                    }
                }
            }
        }

    }
}

private suspend fun io.ktor.server.routing.RoutingContext.readChatStreamRequest(
    mapper: ObjectMapper,
): ChatStreamRequest {
    val body = call.receiveText().trim()
    require(body.isNotBlank()) { "Request body is required" }
    val request = mapper.readValue(body, ChatStreamRequest::class.java)
    require(request.message.isNotBlank()) { "message is required" }
    return request
}

data class ChatStreamRequest(
    val conversationId: String? = null,
    val message: String = "",
)

data class ChatConversationsResponse(
    val conversations: List<ChatConversationResponse>,
)

data class ChatConversationResponse(
    val id: String,
    val title: String?,
    val createTime: String,
    val lastActiveTime: String,
    val turns: List<ChatTurnResponse>,
)

data class ChatTurnResponse(
    val id: String,
    val userInput: String?,
    val modelOutput: String?,
    val createTime: String,
)

private fun String.preview(maxLength: Int = 80): String {
    val oneLine = replace(Regex("\\s+"), " ").trim()
    return if (oneLine.length <= maxLength) oneLine else oneLine.take(maxLength) + "..."
}
