import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
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

    fun outdent() {
        if (padding.length == 0) {
            throw IllegalArgumentException("Called outdent without corresponsing indent")
        }
        padding = padding.removeSuffix(paddingStep)
    }
    fun log(msg: String) {
        bufferedWriter.write(padding + msg + "\n")
        bufferedWriter.flush()
    }
}