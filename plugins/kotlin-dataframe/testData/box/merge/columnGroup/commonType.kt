// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val source = dataFrameOf(
        "left" to columnOf("value" to columnOf(1)),
        "right" to columnOf("value" to columnOf(2.0)),
    )

    val result = source.merge { left and right }.into("result")

    checkExactType<Any>(result[0].result[0].value)
    result.assert()

    return "OK"
}
