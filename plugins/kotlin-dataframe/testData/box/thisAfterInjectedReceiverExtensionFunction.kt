import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Record(val a: String, val b: Int)

fun DataFrame<Record>.check(): Boolean {
    val before = this
    this.select("a").a
    val after: DataFrame<*> = this
    this.select("b").b
    return before === after
}

fun box(): String {
    val df = listOf(Record("112", 42)).toDataFrame()
    return if (df.check()) "OK" else "Fail"
}
