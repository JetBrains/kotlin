// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val left = dataFrameOf(
        "shared" to columnOf(1),
        "leftOnly" to columnOf("left"),
    )
    val right = dataFrameOf(
        "shared" to columnOf(2),
        "rightOnly" to columnOf(false),
    )

    val result = left concat right

    result[0].let { row ->
        checkExactType<Int>(row.shared)
        checkExactType<String?>(row.leftOnly)
        checkExactType<Boolean?>(row.rightOnly)
    }
    result.compareSchemas(strict = true)

    return "OK"
}
