import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val row = dataFrameOf("a" to List(10) { it }).aggregate {
        maxOf { a } into "max"
        minOf { a } into "min"
    }
    val i: Int = row.max
    val i1: Int = row.min
    return "OK"
}
