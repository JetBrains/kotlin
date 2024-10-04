import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class D(
    val s: String
)

fun box(): String {
    val df1 = listOf(D("bb"), null).toDataFrame()
    df1.schema().print()
    df1.compileTimeSchema().print()
    return "OK"
}
