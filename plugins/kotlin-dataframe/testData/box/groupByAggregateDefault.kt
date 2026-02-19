import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val dataFrameOf = dataFrameOf("a" to listOf(1, 1, 2, 3))
    val df = dataFrameOf.groupBy { a }.aggregate { "1" }
    val str: DataColumn<String> = df.aggregated
    df.compareSchemas(strict = true)

    dataFrameOf.aggregate { "1" }.toDataFrame().compareSchemas(strict = true)
    return "OK"
}
