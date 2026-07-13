import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "a" to columnOf(42),
        "b" to columnOf("abc"),
    ).move { a named "c" }.after { b }
    df.checkCompileTimeSchemaEqualsRuntime()
    return "OK"
}
