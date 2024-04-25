import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

data class Record(val a: String, val b: Int)

fun box(): String {
    val df = listOf(Record("112", 42)).toDataFrame(maxDepth = 1)
    val df1 = df.group { a and b }.into("c")
    df1.c.a
    df1.c.b
    return "OK"
}
