import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "col" to columnOf(1, 2, 3, 4),
    )
        .convert { col }.with { listOf(it.toDouble()) }
    val m1 = (0..10).toDataFrame {
        "id" from { it }
    }

    val res = df concat m1

    res.compareSchemas(strict = true)
    return "OK"
}
