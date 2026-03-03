import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    dataFrameOf("a" to columnOf("1")).convert("a").toInt().let { df ->
        val col: DataColumn<Int> = df.a
    }

    DataFrame.readJsonStr("""{"a": "1"}""").add("b") { "2" }.convert("a", "b").toInt().let { df ->
        val col: DataColumn<Int?> = df.a
        assert(df.compileTimeSchema().column("a").nullable)
        val col1: DataColumn<Int> = df.b
    }
    return "OK"
}
