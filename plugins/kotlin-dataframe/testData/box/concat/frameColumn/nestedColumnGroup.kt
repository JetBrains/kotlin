// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val left = dataFrameOf(
        "frameCol" to columnOf(
            dataFrameOf(
                "innerGroup" to columnOf(
                    "common" to columnOf(1),
                    "leftOnly" to columnOf(true),
                ),
            ),
        ),
    )
    val right = dataFrameOf(
        "frameCol" to columnOf(
            dataFrameOf(
                "innerGroup" to columnOf(
                    "common" to columnOf(2),
                    "rightOnly" to columnOf("right"),
                ),
            ),
        ),
    )

    val result = left concat right

    result.compareSchemas(strict = true)

    result.frameCol[0].innerGroup[0].let { nestedGroupRow ->
        checkExactType<Int>(nestedGroupRow.common)
    }
    return "OK"
}
