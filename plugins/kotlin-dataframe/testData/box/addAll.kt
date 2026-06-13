import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "a" to columnOf(1, 2, 3)
    )
    val df1 = dataFrameOf(
        "b" to columnOf("1", "2", "3")
    )
    val df2 = DataFrame.empty(3)
    val res = df.addAll(df1, df2)
    val i: Int = res[0].a
    val str: String = res[0].b
    res.compareSchemas(strict = true)
    return "OK"
}
