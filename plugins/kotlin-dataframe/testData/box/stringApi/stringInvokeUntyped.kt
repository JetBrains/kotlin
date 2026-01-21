import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    DataFrame.readJsonStr("{\"a\":1, \"b\":2}").select { "a"() and "b"() }.let { df ->
        val col: DataColumn<Any?> = df.a
        val col1: DataColumn<Any?> = df.b
        df.assert()
    }
    return "OK"
}
