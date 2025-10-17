import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    DataFrame.readJsonStr("{\"a\":1, \"b\":2}").select("a", "b").let { df ->
        val col: DataColumn<Any?> = df.a
        val col1: DataColumn<Any?> = df.b
        df.assert()
    }

    DataFrame.readJsonStr("{\"a\":1, \"b\":2}").add("c") { 123 }.select("a", "b", "c").let { df ->
        val col: DataColumn<Any?> = df.a
        val col1: DataColumn<Any?> = df.b
        val col2: DataColumn<Int> = df.c
        df.assert()
    }
    return "OK"
}
