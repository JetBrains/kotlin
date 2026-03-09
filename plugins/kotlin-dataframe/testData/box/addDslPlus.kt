import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a" to columnOf("123")).mapToFrame {
        +a
        "b" from { a }
    }
    df.a
    df.compareSchemas()
    return "OK"
}
