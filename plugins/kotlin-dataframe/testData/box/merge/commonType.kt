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

    val result = source.merge { int and double }.into("value")

    checkExactType<List<Any>>(result[0].value)
    result.assert()

    return "OK"
}
