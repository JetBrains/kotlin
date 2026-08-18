// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val left = dataFrameOf(
        "group" to columnOf(
            "shared" to columnOf(1),
            "leftOnly" to columnOf(true),
        ),
    )
    val right = dataFrameOf(
        "group" to columnOf(
            "shared" to columnOf(2),
            "rightOnly" to columnOf("right"),
        ),
    )

    val result = left concat right

    result[0].group.let { group ->
        checkExactType<Int>(group.shared)
        checkExactType<Boolean?>(group.leftOnly)
        checkExactType<String?>(group.rightOnly)
    }
    result.compareSchemas(strict = true)

    return "OK"
}
