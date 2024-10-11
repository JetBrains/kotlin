import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

const val text = """{"a":"abc", "b":1}"""

fun box(): String {
    val row = DataRow.readJsonStr(text)
    row.a
    row.b
    return "OK"
}
