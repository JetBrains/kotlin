// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val result = dataFrameOf(
        "values" to columnOf(listOf(1, 2)),
    ) concat dataFrameOf(
        "values" to columnOf(3),
    )

    result[0].let { row ->
        checkExactType<List<Int>>(row.values)
    }
    result.compareSchemas(strict = true)
    return "OK"
}
