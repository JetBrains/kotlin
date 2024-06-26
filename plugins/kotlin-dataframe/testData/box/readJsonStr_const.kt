import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

const val text = """[{"a":null, "b":1},{"a":null, "b":2}]"""

fun box(): String {
    val df = DataFrame.readJsonStr(text)
    df.a
    df.b
    return "OK"
}
