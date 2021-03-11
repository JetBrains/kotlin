// WITH_RUNTIME
import java.io.File
import java.io.BufferedReader

fun bar() {}

fun foo(reader: BufferedReader?) {
    <caret>try {
        reader?.readLine()
        bar()
    }
    finally {
        reader?.close()
    }
}