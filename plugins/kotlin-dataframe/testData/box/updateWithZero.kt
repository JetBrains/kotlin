import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df1 = dataFrameOf("a" to columnOf(1, null)).update { a }.withZero()
    val a1: DataColumn<Int> = df1.a

    val df2 = dataFrameOf("a" to columnOf(1.0, null)).fillNulls { a }.withZero()
    val a2: DataColumn<Double> = df2.a
    df2.compareSchemas(strict = true)
    return "OK"
}
