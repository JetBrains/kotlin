import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*

@DataSchema
interface Add0 {
    val a: Int
}

val ColumnsContainer<Add0>.a: DataColumn<Int>  get() = this["a"] as DataColumn<Int>
val DataRow<Add0>.a: Int  get() = this["a"] as Int

fun add0(df: DataFrame<Add0>) {
    test(id = "add0_schema", call = dataFrame(df))
    val df1 = test(id = "add0", call = df.add("") { 42 })
    fun col0(v: kotlin.Int) {}
    col0(df1.a[0])
    fun col1(v: kotlin.Int) {}
    col1(df1.untitled[0])
}