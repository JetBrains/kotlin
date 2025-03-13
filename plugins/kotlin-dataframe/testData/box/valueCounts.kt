import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a" to listOf("1", "1", "2", "3", "1", "4", "1"))
    val counts = df.valueCounts(ascending = true, sort = false, dropNA = false) { a }
    counts.a
    counts.count

    val counts1 = df.valueCounts(resultColumn = "a") { a }
    val col: DataColumn<String> = counts1.a
    val col1: DataColumn<Int> = counts1.a1
    return "OK"
}
