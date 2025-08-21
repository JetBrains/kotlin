import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val group = columnOf(
        "c" to columnOf("2"),
        "d" to columnOf(123),
    )
    val str: DataColumn<String> = group.c
    val i: DataColumn<Int> = group.d

    return "OK"
}
