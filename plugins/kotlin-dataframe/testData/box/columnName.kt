import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Record(
    @ColumnName("a")
    val abc: String,
)

fun box(): String {
    val df = dataFrameOf("a")("1").cast<Record>()
    df.abc

    val df1 = df.add("b") { 1 }
    df1.a
    return "OK"
}
