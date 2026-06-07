package life.main

import io.github.cdimascio.dotenv.Dotenv
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import life.api.chatRoutes
import life.api.healthRoutes
import life.main.startup.Agent
import life.main.startup.AgentDependencies
import life.main.startup.DatabaseStartup
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("life.Main")

fun main() {
    val dotenv = Dotenv.load()
    DatabaseStartup.start(dotenv)
    val agent = Agent.start(dotenv)
    val port = dotenv["PORT"].toInt()
    logger.info("Starting Life server on port {}", port)

    embeddedServer(Netty, port = port) {
        lifeApplication(agent)
    }.start(wait = true)
}

fun Application.lifeApplication(agent: AgentDependencies) {
    install(CallLogging)
    healthRoutes()
    chatRoutes(
        mainLoopFactory = agent.mainLoopFactory,
        conversationService = agent.conversationService,
        mapper = agent.mapper,
    )
}
