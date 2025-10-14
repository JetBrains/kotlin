import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "a" to columnOf(1, 2, null, 3),
        "b" to columnOf("a", "a", "b", "b")
    )

    val res = df.convert { a named "d" }.toDouble()
    val d: DataColumn<Double?> = res.d
    res.compareSchemas(strict = true)
    return "OK"
}
