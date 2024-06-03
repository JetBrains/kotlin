import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

data class Record(val a: String, val b: Int)

fun box(): String {
    val df = listOf(Record("112", 42)).toDataFrame(maxDepth = 1)
    val df1 = df.select { this.a }
    df1.a
    return "OK"
}
