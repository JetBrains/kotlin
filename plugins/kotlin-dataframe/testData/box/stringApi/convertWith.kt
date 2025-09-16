import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    DataFrame.readJsonStr("""{"a": "1"}""").add("b") { "2" }.convert("a", "b") { (it as String).toInt() }.let { df ->
        val col: DataColumn<Int> = df.a
        val col1: DataColumn<Int> = df.b
        df.compareSchemas(strict = true)
    }
    return "OK"
}
