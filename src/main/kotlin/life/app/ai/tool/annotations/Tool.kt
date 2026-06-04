package life.app.ai.tool.annotations

@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Tool(
    val name: String = "",
    val description: String = "",
)
