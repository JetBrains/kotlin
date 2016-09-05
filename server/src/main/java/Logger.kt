import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

object Logger {
    val file = File("debug.log")
    val fileWriter = FileWriter(file.absoluteFile)
    val bufferedWriter = BufferedWriter(fileWriter)
    var padding = ""
    var paddingStep = "  "

    fun indent() {
        padding += paddingStep
    }

    fun outDent() {
        if (padding.length == 0) {
            throw IllegalArgumentException("Called out dent without corresponding indent")
        }
        padding = padding.removeSuffix(paddingStep)
    }

    fun log(msg: String) {
        bufferedWriter.write(padding + msg + "\n")
        bufferedWriter.flush()
    }
}