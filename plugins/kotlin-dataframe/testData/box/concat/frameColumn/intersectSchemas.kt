// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val left = dataFrameOf(
        "sharedFrame" to columnOf(
            dataFrameOf(
                "common" to columnOf(1),
                "leftOnly" to columnOf(true),
            ),
        ),
    )
    val right = dataFrameOf(
        "sharedFrame" to columnOf(
            dataFrameOf(
                "common" to columnOf(2),
                "rightOnly" to columnOf("right"),
            ),
        ),
    )

    val result = left concat right

    val row = result[0]

    result.compareSchemas(strict = true)

    row.sharedFrame[0].let { sharedFrameRow ->
        checkExactType<Int>(sharedFrameRow.common)
    }
    return "OK"
}
