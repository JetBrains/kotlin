import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val raw = DataFrame.readJsonStr("{\"a\":1}")

    raw.select { "a"() named "c" }.let { df ->
        val col: DataColumn<Any?> = df.c
        df.assert()
    }

    raw
        .add("b") { "abc" }
        .move { "a"() named "d" }
        .after { b }
        .let { df ->
            val col: DataColumn<Any?> = df.d
            df.assert()
        }

    return "OK"
}
