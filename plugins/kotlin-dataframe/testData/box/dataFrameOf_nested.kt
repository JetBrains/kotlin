import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import kotlin.reflect.typeOf

fun box(): String {
    val df = dataFrameOf(
        "a" to columnOf("1"),
        "b" to columnOf(
            "c" to columnOf("2"),
        ),
        "d" to columnOf(dataFrameOf("a")(123)),
        "gr" to listOf("1").toDataFrame().asColumnGroup(),
        "valueCol" to DataColumn.createValueColumn("", listOf(1), typeOf<Int>()),
    )
    val str: DataColumn<String> = df.a
    val str1: DataColumn<String> = df.b.c
    val i: DataColumn<Int> = df.d[0].a
    val str2: DataColumn<String> = df.gr.value
    return "OK"
}
