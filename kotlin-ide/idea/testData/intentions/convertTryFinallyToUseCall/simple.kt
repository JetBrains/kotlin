// WITH_RUNTIME
import java.io.File

fun main(args: Array<String>) {
    val reader = File("hello-world.txt").bufferedReader()
    try <caret>{
        reader.readLine()
    }
    finally {
        reader.close()
    }
}