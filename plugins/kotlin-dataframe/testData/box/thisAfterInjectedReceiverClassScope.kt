import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Record(val a: String, val b: Int)

class Holder(val df: DataFrame<Record>) {
    fun check(): Boolean {
        val before = this
        this.df.select("a").a
        val after: Holder = this
        this.df.select("b").b
        return before === after
    }
}

fun box(): String {
    val df = listOf(Record("112", 42)).toDataFrame()
    return if (Holder(df).check()) "OK" else "Fail"
}
