import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

data class Nested(val i: Double?)

data class Record(val a: String?, val b: Int?, val nested: Nested)

fun box(): String {
    val df = listOf(Record("112", 42, Nested(3.0))).toDataFrame(maxDepth = 1)
    df.dropNulls { a and b }

    return "OK"
}
