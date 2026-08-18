// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val result = dataFrameOf(
        "nested" to columnOf("value" to columnOf(1)),
    ) concat dataFrameOf(
        "nested" to columnOf(dataFrameOf("value")(2)),
    )

    result[1].nested[0].let { nestedRow ->
        val value: Int = nestedRow.value
    }
    result.compareSchemas(strict = true)

    return "OK"
}
