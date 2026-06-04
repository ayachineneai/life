package life.app.ai.tool.fixtures.valid

import life.app.ai.tool.annotations.Tool

class FileTools {
    @Tool(name = "list_files")
    fun list(args: ListFilesArgs): String {
        return "${args.path}:${args.recursive}:${args.limit}"
    }
}

data class ListFilesArgs(
    val path: String,
    val recursive: Boolean,
    val limit: Int?,
)
