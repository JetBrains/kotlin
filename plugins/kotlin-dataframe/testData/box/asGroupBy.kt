import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("i", "group")(1, dataFrameOf("a", "b")(111, 222))
    val aggregated1 = df.asGroupBy { group }.aggregate { maxOf { a } into "max" }
    val aggregated2 = df.asGroupBy().aggregate { maxOf { a } into "max" }

    val i: Int = aggregated1.max[0]

    compareSchemas(aggregated1, aggregated2)
    return "OK"
}
