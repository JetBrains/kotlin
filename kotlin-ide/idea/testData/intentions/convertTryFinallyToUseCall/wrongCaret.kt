// IS_APPLICABLE: false
// WITH_RUNTIME
import java.io.File

fun main(args: Array<String>) {
    val reader = File("hello-world.txt").bufferedReader()
    try {
        reader.readLine()
    }
    finally {
        <caret>reader.close()
    }
}