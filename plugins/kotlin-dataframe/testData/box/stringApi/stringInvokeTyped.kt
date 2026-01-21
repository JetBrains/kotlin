import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    DataFrame.readJsonStr("{\"a\":1, \"b\":2}").select { "a"<Int>() and "b"<Int>() }.let { df ->
        val col: DataColumn<Int> = df.a
        val col1: DataColumn<Int> = df.b
        df.assert()
    }
    return "OK"
}
