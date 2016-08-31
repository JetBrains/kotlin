import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

object Logger {
    val file = File("debug.log")
    val fileWriter = FileWriter(file.absoluteFile)
    val bufferedWriter = BufferedWriter(fileWriter)

    fun log(msg: String) {
        bufferedWriter.write(msg + "\n")
        bufferedWriter.flush()
    }
}