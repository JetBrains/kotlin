import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface Cars {
    val year: Int
}

fun `Name is evaluated to age`(df: DataFrame<Cars>) {
    val df1 = df.add("age") { 2022 - year }
    val col = df1.age
}

fun `ReturnType is evaluated to Int`(df: DataFrame<Cars>) {
    val df1 = df.add("age") { 2022 - year }
    val col: DataColumn<Int> = df1.age
}

fun box() = "OK"
