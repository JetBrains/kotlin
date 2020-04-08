// WITH_RUNTIME
import java.io.File
import java.io.BufferedReader

fun BufferedReader.foo() {
    try <caret>{
        readLine()
    }
    finally {
        close()
    }
}