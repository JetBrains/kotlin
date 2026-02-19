import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

data class Nested(val d: List<Double>)

data class Record(val a: String, val b: Int, val nested: List<Nested>)

fun box(): String {
    val df = listOf(Record("112", 42, listOf(Nested(listOf(3.0))))).toDataFrame(maxDepth = 1)

    df.group { nested }.into("group").convert { colsAtAnyDepth().frameCols() }.with { 1 }.compareSchemas()
    df.group { b }.into("group").convert { colsAtAnyDepth().colsOf<Int>() }.with { "" }.compareSchemas()
    df.convert { all().frameCols() }.with { 1 }.compareSchemas()
    return "OK"
}
