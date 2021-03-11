// IS_APPLICABLE: false
// WITH_RUNTIME
import java.io.File

fun main(args: Array<String>) {
    val reader = File("hello-world.txt").bufferedReader()
    <caret>try {
        reader.readLine()
    }
    finally {
        reader.readLine()
    }
}