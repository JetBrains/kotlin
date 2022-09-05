import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

interface Cars

fun manual(df: DataFrame<Cars>) {
    //val df1 = df.add("age") { 2022 }
    class `99` {
        val ColumnsContainer<Cars>.age get() = this["age"] as DataColumn<Int>
        val DataRow<Cars>.age get() = this["age"] as Int
    }
    with(`99`()) {
        val col = df.age
    }
}

fun generated(df: DataFrame<Cars>) {
    val df1 = df.add("age") { 2022 }
    val col = df1.age
}