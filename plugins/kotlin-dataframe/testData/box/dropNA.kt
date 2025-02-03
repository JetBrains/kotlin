import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "a" to listOf(1, null, 3),
        "b" to listOf(null, 5, 6)
    )
    val df1 = df.dropNA { a and b }
    df1.compareSchemas(strict = true)

    val df2 = df.dropNA(whereAllNA = true) { a and b }
    df2.compareSchemas(strict = true)
    return "OK"
}
