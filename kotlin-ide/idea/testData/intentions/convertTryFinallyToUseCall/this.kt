// WITH_RUNTIME
import java.io.File
import java.io.BufferedReader

fun BufferedReader.foo() {
    <caret>try {
        this.readLine()
    }
    finally {
        this.close()
    }
}