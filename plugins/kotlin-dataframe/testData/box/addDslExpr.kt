// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a" to columnOf(1))

    df.add {
        expr { a + 1 } into "b"
    }.let {
        checkExactType<Int>(it.b[0])
        it.compareSchemas(strict = true)
    }
    return "OK"
}
