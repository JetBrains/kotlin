import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Record(val a: String, val b: Int)

fun box(): String {
    val df = List(10) { Record(it.toString(), it) }.let { dataFrameOf(*it.toTypedArray()) }
    val df1 = df.groupBy { b }.aggregate { "123" into "fsdf" }

    val nested = df.group { a and b }.into("c")
    val df2 = nested.groupBy { c.a }.aggregate { "123" into "ff" }
    df2.a

    val df3 = nested.groupBy(moveToTop = false) { c.a }.aggregate { "123" into "ff" }
    df3.c.a
    return "OK"
}
