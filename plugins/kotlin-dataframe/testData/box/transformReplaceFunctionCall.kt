import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    test()
    return "OK"
}

fun test() {
    val df = dataFrameOf("a")(1)
    df.add("col1") { 42 }.col1.print()
}
