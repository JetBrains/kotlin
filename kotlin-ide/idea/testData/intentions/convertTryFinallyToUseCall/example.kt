// WITH_RUNTIME
import java.io.File

fun main(args: Array<String>) {
    val reader = File("hello-world.txt").bufferedReader()
    <caret>try {
        // do stuff with reader
    }
    finally {
        reader.close()
    }
}