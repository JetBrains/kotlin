import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
data class Record(val a: String, val b: Int)

fun box(): String {
    val df = listOf(Record("112", 42)).toDataFrame(maxDepth = 1)
    val df1 = df.group { a and b }.into("c")
    df1.c.a
    df1.c.b

    val df2 = df1.group { c }.into("d")
    df2.d.c.a

    val df3 = df2.ungroup { d.c and d }
    df3.c.a
    df3.c.b

    val df4 = df2.ungroup { d }.ungroup { c }
    df4.a
    df4.b
    return "OK"
}
