import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a")(1, 2, 3)
    val i: Int = df.a[0]

    val df1 = dataFrameOf("a", "b")({ 1 }, 2)
    val i1: Int = df1.a[0].invoke()
    return "OK"
}
