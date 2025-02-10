import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a")(1, 1, 2, 3, 3).groupBy { a }.count()
    val i: Int = df.count[0]

    val df1 = dataFrameOf("a")(1, 1, 2, 3, 3).groupBy { a }.count { a > 1 }
    val i1: Int = df1.count[0]

    val df2 = dataFrameOf("a")(1, 1, 2, 3, 3).groupBy { a }.count("myCol") { a > 1 }
    val i2: Int = df2.myCol[0]
    return "OK"
}
