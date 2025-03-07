import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Record(val id: String, val b: Int)

public data class NameValuePair<V>(val name: String, val value: V)

fun box(): String {
    val df = dataFrameOf(Record("1", 1), Record("2", 123), Record("3", 321))
    val df1 = df.first().transpose().dropNulls { value }
    val v: Any = df1.value[0]
    return "OK"
}
