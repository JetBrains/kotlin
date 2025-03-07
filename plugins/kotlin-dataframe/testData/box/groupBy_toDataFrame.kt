import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Record(val a: String, val b: Int)

fun box(): String {
    val df = List(10) { Record(it.toString(), it) }.let { dataFrameOf(*it.toTypedArray()) }
    val df1 = df.groupBy { b }.toDataFrame()
    df1.group[0].a
    df1.group[0].b

    val df2 = df.groupBy { b }.into("gr")
    df2.gr[0].a
    df2.gr[0].b
    return "OK"
}
