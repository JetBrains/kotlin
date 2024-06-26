import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Record(val id: String, val b: Int)

fun box(): String {
    val df = dataFrameOf(Record("1", 1), Record("2", 123), Record("3", 321))
    val df1 = listOf("1", "2", "3").toDataFrame().join(df) { value match right.id }
    val col: DataColumn<String> = df1.value
    return "OK"
}
