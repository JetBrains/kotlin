import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@Refine
@Interpretable("DataFrameGenerator")
@DisableInterpretation
fun DataFrame(size: Int, body: CreateDataFrameDsl<Int>.() -> Unit): DataFrame<Any> {
    return (0 until size).<!DATAFRAME_PLUGIN_IS_DISABLED!>toDataFrame<!>(body)
}

fun box(): String {
    val df = DataFrame(5) {
        "index" from { it }
        "pow" from { (it * it).toDouble() }
    }

    df.compareSchemas(strict = true)
    return "OK"
}
