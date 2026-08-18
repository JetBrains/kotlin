// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val source = dataFrameOf(
        "left" to columnOf(
            "frame" to columnOf(dataFrameOf("value")(1)),
        ),
        "right" to columnOf(
            "frame" to columnOf(dataFrameOf("value")(2.0)),
        ),
    )

    val result = source.merge { left and right }.into("result")

    result[0].result[0].let { mergedRow ->
        mergedRow.frame[0].let { frameRow ->
            checkExactType<Any>(frameRow.value)
        }
    }
    result.assert()

    return "OK"
}
