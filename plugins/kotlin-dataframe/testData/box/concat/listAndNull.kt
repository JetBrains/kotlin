// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val result = dataFrameOf(
        "values" to columnOf(listOf(1, 2)),
    ) concat dataFrameOf(
        "values" to columnOf(null as Int?),
    )

    checkExactType<List<Int>?>(result[0].values)
    result.compareSchemas(strict = true)
    return "OK"
}
