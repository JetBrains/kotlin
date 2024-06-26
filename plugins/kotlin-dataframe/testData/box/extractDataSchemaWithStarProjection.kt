import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Record(val id: String, val b: Int)

public data class NameValuePair<V>(val name: String, val value: V)

fun box(): String {
    val df = dataFrameOf(Record("1", 1), Record("2", 123), Record("3", 321))
    val df1 = df.first().transpose().add("c") { 1 }
    df1.name
    df1.value
    df1.c
    return "OK"
}
