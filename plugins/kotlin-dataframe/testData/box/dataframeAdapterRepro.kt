import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df1 = dataFrameOf(
        "a" to columnOf("1"),
        "b" to columnOf(dataFrameOf("c" to columnOf(2))),
    )

    val df2 = df1.split { b }.by { this.b.rows() }.inplace()
    val i: Int = df2[0].b[0].c
    df2.compareSchemas(strict = true)
    return "OK"
}
