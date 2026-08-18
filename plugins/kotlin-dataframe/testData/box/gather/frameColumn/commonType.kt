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

    val df = source.gather { left and right }.valuesInto("result")

    df[0].result.let { resultRow ->
        resultRow.frame[0].let { frameRow ->
            checkExactType<Any>(frameRow.value)
        }
    }
    df.assert()

    return "OK"
}
