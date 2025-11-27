import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class D(
    val s: String
)

fun box(): String {
    val df = listOf(D("bb"), null).toDataFrame()
    val str: String? = df[0].s
    df.compareSchemas(strict = true)
    return "OK"
}
