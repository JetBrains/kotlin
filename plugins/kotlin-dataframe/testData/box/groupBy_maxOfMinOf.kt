import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a")(1, 1, 2, 3, 3).groupBy { a }.add("id") { index() }.maxOf { 123 }
    val df1 = dataFrameOf("a")(1, 1, 2, 3, 3).groupBy { a }.add("id") { index() }.minOf { 123 }

    val max = df.max[0]
    val min = df1.min[0]

    df.compareSchemas()
    df1.compareSchemas()

    val df2 = dataFrameOf("a")(1, 1, 2, 3, 3).groupBy { a }.add("id") { index() }.maxOf("myMax") { 123 }
    val df3 = dataFrameOf("a")(1, 1, 2, 3, 3).groupBy { a }.add("id") { index() }.minOf("myMin") { 123 }

    df2.myMax
    df3.myMin
    return "OK"
}
