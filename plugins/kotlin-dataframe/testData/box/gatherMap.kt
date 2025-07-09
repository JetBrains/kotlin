import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "a" to columnOf(listOf(1, 2)),
        "b" to columnOf(listOf(3, 4)),
    )
    val res = df.gather { a and b }.explodeLists().cast<Int>().mapKeys { listOf(it) }.mapValues { listOf(it) }.into("key", "value")

    val key: DataColumn<List<String>> = res.key
    val value: DataColumn<List<Int>> = res.value
    return "OK"
}
