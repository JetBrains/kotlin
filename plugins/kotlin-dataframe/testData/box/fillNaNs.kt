import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    dataFrameOf("a" to columnOf(1.0, Double.NaN)).fillNaNs { a }.with { 1.0 }.let { df ->
        df.compareSchemas(strict = true)
    }
    return "OK"
}
