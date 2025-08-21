import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b")(1, null, null, "")
    val df1 = df.fillNulls { b }.with { "empty" }
    val b: DataColumn<String> = df1.b

    val df2 = df.fillNA { b }.with { "empty" }
    val b1: DataColumn<String> = df2.b
    return "OK"
}
