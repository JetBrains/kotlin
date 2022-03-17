import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

interface Cars

val DataRow<Cars>.year: Int get() = this["age"] as Int
val ColumnsContainer<Cars>.year: DataColumn<Int> get() = this["age"] as DataColumn<Int>

fun `Name is evaluated to age`(df: DataFrame<Cars>) {
    val df1 = df.add("age") { 2022 - year }
    val col = df1.age
}

fun `ReturnType is evaluated to Int`(df: DataFrame<Cars>) {
    val df1 = df.add("age") { 2022 - year }
    val col: DataColumn<Int> = df1.age
}