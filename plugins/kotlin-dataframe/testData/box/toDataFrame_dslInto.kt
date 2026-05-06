import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = listOf(1, 2, 3).toDataFrame {
        expr { it.toString() } into "values"
    }
    val str: String = df[0].values
    return "OK"
}
