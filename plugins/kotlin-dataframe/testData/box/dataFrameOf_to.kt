import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        Pair("a", listOf(1, 2)),
        "b" to listOf("str1", "str2"),
    )
    val i: Int = df.a[0]
    val str: String = df.b[0]
    return "OK"
}
