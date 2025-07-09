import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "a" to columnOf(listOf(1, 2)),
        "b" to columnOf(listOf(3, 4)),
    )
    val res = df.gather { a and b }.explodeLists().into("key", "value")

    val key: DataColumn<String> = res.key
    val value: DataColumn<Int> = res.value
    return "OK"
}
