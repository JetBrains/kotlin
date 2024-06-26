import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val text = """[{"a":null, "b":1},{"a":null, "b":2}]"""
    val df = DataFrame.readJsonStr(text)
    df.a
    df.b
    return "OK"
}
