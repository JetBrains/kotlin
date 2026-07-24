import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = listOf(1, 2, 3).toDataFrame {
        expr { it.toString() } into "values"
        expr { it } into pathOf("a", "b")
        "group" {
            expr { it } into pathOf("a", "b")
        }
    }
    val str: String = df[0].values
    val i1: Int = df[0].a.b
    val i2: Int = df[0].group.a.b
    df.compareSchemas(strict = true)
    return "OK"
}
