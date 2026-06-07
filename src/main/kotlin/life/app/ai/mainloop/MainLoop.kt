package life.app.ai.mainloop

import com.openai.client.OpenAIClient
import life.app.ai.conversation.ConversationService
import life.app.ai.http.sendAgentEvent
import life.app.ai.mainloop.data.ActResult
import life.app.ai.mainloop.data.PreparedTurn
import life.app.ai.mainloop.data.RespondResult
import life.app.ai.mainloop.data.ThinkResult
import life.app.ai.mainloop.support.AgentStreamEvents
import life.app.ai.mainloop.support.AgentTools
import life.app.ai.mainloop.support.MainLoopConfig
import life.app.ai.mainloop.support.TurnPhase
import life.app.ai.mainloop.support.TurnStatus
import life.app.ai.mainloop.support.ToolState
import life.app.ai.utils.AiRequests
import life.app.ai.utils.AiRuns
import life.app.ai.utils.hasToolCall
import life.app.ai.utils.outputText
import life.infra.sse.SseWriter
import kotlin.uuid.Uuid

class MainLoop(
    private val client: OpenAIClient,
    private val conversationService: ConversationService,
    private val tools: AgentTools,
    private val config: MainLoopConfig,
    private val sse: SseWriter,
) {
    fun mainLoop(
        conversationId: Uuid?,
        message: String,
    ): RespondResult {
        val preparedTurn = prepareConversation(conversationId, message)
        val thinkResult = think(preparedTurn)
        act(preparedTurn, thinkResult)
        return respond(preparedTurn)
    }

    fun prepareConversation(
        conversationId: Uuid?,
        message: String,
    ): PreparedTurn {
        sse.sendAgentEvent(AgentStreamEvents.status(TurnStatus.PREPARING))
        val conversation = if (conversationId == null) {
            conversationService.createConversation(title = message.trim().take(config.titleMaxLength))
        } else {
            checkNotNull(conversationService.findConversationById(conversationId)) {
                "Conversation not found: $conversationId"
            }
        }
        sse.sendAgentEvent(
            AgentStreamEvents.conversation(
                conversationId = conversation.id.toString(),
            ),
        )

        return PreparedTurn(
            message = message,
            conversationId = conversation.id,
            openAiConversationId = conversation.openaiConversationId,
        )
    }

    fun think(preparedTurn: PreparedTurn): ThinkResult {
        sse.sendAgentEvent(AgentStreamEvents.status(TurnStatus.THINKING))
        val text = StringBuilder()

        fun deltaConsumer(delta: String) {
            text.append(delta)
            sse.sendAgentEvent(AgentStreamEvents.planDelta(delta))
        }

        val response = AiRuns.runStream(
            client = client,
            params = AiRequests.conversationText(
                conversationId = preparedTurn.openAiConversationId,
                text = preparedTurn.message,
                model = config.plannerModel,
                systemPrompt = """
                    Create a concise action plan for the current user request.
                    Do not call tools in this phase.
                    Do not answer the user directly.
                    Output only the plan text.
                """.trimIndent(),
            ),
            handler = { event ->
                event.outputTextDelta().ifPresent { delta -> deltaConsumer(delta.delta()) }
                event.refusalDelta().ifPresent { delta -> deltaConsumer(delta.delta()) }
                event.error().ifPresent { error ->
                    sse.sendAgentEvent(AgentStreamEvents.error(error.message()))
                }
            },
        )

        if (text.isEmpty()) {
            response.outputText().takeIf { it.isNotBlank() }?.let { output ->
                deltaConsumer(output)
            }
        }

        return ThinkResult(
            responseId = response.id(),
            text = text.toString(),
        )
    }

    fun act(
        preparedTurn: PreparedTurn,
        thinkResult: ThinkResult,
    ): ActResult {
        var params = AiRequests.conversationText(
            conversationId = preparedTurn.openAiConversationId,
            text = "Execute the plan for the current user request.",
            model = config.executorModel,
            systemPrompt = """
                Execute the plan below for the current user request.
                Use available tools when needed.
                Do not provide the final user-facing response in this phase.
                When no more tool calls are needed, output exactly ACTION_COMPLETE.
    
                Plan:
                ${thinkResult.text}
            """.trimIndent(),
            tools = tools.registry.openAiTools(),
        )

        repeat(config.maxExecutorSteps) {
            val response = AiRuns.runStream(
                client = client,
                params = params,
                handler = {},
            )
            if (!response.hasToolCall()) {
                return ActResult(
                    responseId = response.id(),
                    text = response.outputText(),
                )
            }

            val outputs = response.output().mapNotNull { item ->
                item.functionCall().ifPresent { call ->
                    sse.sendAgentEvent(AgentStreamEvents.status(TurnStatus.USING_TOOL))
                    val knownTool = tools.registry.get(call.name()) != null
                    sse.sendAgentEvent(
                        AgentStreamEvents.tool(
                            phase = TurnPhase.ACT,
                            state = if (knownTool) ToolState.EXECUTING else ToolState.MISSING_TOOL,
                            callId = call.callId(),
                            name = call.name(),
                            arguments = call.arguments(),
                        ),
                    )
                }
                tools.handler.handle(item)?.also { output ->
                    sse.sendAgentEvent(
                        AgentStreamEvents.tool(
                            phase = TurnPhase.ACT,
                            state = ToolState.RESULT,
                            callId = output.callId,
                            result = output.result,
                        ),
                    )
                }
            }

            params = AiRequests.toolOutputs(
                conversationId = preparedTurn.openAiConversationId,
                model = config.executorModel,
                outputs = outputs,
                tools = tools.registry.openAiTools(),
            )
        }

        error("Act tool loop limit reached: ${config.maxExecutorSteps}")
    }

    fun respond(preparedTurn: PreparedTurn): RespondResult {
        sse.sendAgentEvent(AgentStreamEvents.status(TurnStatus.RESPONDING))
        val text = StringBuilder()

        fun deltaConsumer(delta: String) {
            text.append(delta)
            sse.sendAgentEvent(AgentStreamEvents.delta(delta))
        }

        val response = AiRuns.runStream(
            client = client,
            params = AiRequests.conversationText(
                conversationId = preparedTurn.openAiConversationId,
                text = "Provide the final user-facing response for the current user request.",
                model = config.executorModel,
                systemPrompt = """
                    Provide the final user-facing response for the current user request.
                    Use the prior plan and any tool results already in the conversation.
                    Do not mention internal planning or ACTION_COMPLETE.
                """.trimIndent(),
            ),
            handler = { event ->
                event.outputTextDelta().ifPresent { delta -> deltaConsumer(delta.delta()) }
                event.refusalDelta().ifPresent { delta -> deltaConsumer(delta.delta()) }
                event.error().ifPresent { error ->
                    sse.sendAgentEvent(AgentStreamEvents.error(error.message()))
                }
            },
        )

        if (text.isEmpty()) {
            response.outputText().takeIf { it.isNotBlank() }?.let { output ->
                deltaConsumer(output)
            }
        }

        return RespondResult(
            responseId = response.id(),
            text = text.toString(),
        )
    }

}
