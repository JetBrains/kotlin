// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val result = dataFrameOf(
        "group" to columnOf("value" to columnOf(1)),
    ) concat dataFrameOf(
        "group" to columnOf("value" to columnOf(2.0)),
    )

    checkExactType<Any>(result[0].group.value)
    result.assert()

    return "OK"
}
