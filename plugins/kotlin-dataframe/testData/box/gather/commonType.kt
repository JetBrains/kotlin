// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val source = dataFrameOf(
        "int" to columnOf(1),
        "double" to columnOf(2.0),
    )

    val result = source.gather { int and double }.valuesInto("value")

    checkExactType<Any>(result[0].value)
    result.assert()

    return "OK"
}
