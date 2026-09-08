import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "java" to listOf(1, 2, 3),
        "kotlin" to listOf(1, null, 3),
    )

    val res = df.gather { java and kotlin }.notNull().where { it % 2 == 0 }.into("key", "value")
    val key: DataColumn<String> = res.key
    val value: DataColumn<Int> = res.value
    return "OK"
}
