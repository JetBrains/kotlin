import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

data class Record(val a: String, val b: Int)

fun box(): String {
    val df = listOf(Record("112", 42)).toDataFrame(maxDepth = 1)
    val df1 = df.select { it.a }
    df1.a
    return "OK"
}
