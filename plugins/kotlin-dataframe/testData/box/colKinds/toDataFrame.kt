import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

@DataSchema
data class Record(val str: String)

class A

fun box(): String {
    val df = dataFrameOf(Record("abc"))
    val df1 = listOf(A()).toDataFrame {
        "dataRow" from { df.first() }
        "dataFrame" from { df }
    }
    val a: String = df1.dataRow.str[0]
    val b: String = df1.dataFrame[0].str[0]
    return "OK"
}
