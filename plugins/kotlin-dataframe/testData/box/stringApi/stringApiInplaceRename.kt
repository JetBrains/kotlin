import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    DataFrame.readJsonStr("{\"a\":1}").select { "a"() named "c" }.let { df ->
        val col: DataColumn<Any?> = df.c
        df.assert()
    }
    return "OK"
}