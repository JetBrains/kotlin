package ideacompose

import java.io.File
import java.lang.System.exit

fun main() {
    val workingDirectory = File(".")

    Patcher(
        workingDirectory,
        workingDirectory[".."],
        workingDirectory["../substitutions.json"]
    ).patch()
}