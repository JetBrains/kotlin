import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "a" to listOf(1),
        "b" to listOf(1),
    ).group { a and b }.into("gr")
    val res = df.select { gr.select { a and b } }
    res.a
    res.b
    return "OK"
}
