package life.main

import io.github.cdimascio.dotenv.Dotenv
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import life.app.ai.AgentRuntime
import life.app.ai.AgentRuntimes
import life.app.ai.http.agentRoutes
import life.main.startup.DatabaseStartup
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("life.Main")

fun main() {
    val dotenv = Dotenv.load()
    DatabaseStartup.start(dotenv)
    val runtime = AgentRuntimes.create(dotenv)
    val port = AgentRuntimes.port(dotenv)
    logger.info("Starting Life Agent server on port {}", port)

    embeddedServer(Netty, port = port) {
        agentApplication(runtime)
    }.start(wait = true)
}

fun Application.agentApplication(runtime: AgentRuntime) {
    install(CallLogging)
    agentRoutes(
        mainLoopFactory = runtime.mainLoopFactory,
        conversationService = runtime.conversationService,
        mapper = runtime.mapper,
    )
}
