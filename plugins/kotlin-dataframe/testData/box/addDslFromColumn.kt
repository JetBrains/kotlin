// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a" to columnOf("x"), "b" to columnOf(1))

    df.mapToFrame {
        "renamed" from b
    }.let {
        checkExactType<Int>(it.renamed[0])
        it.compareSchemas(strict = true)
    }
    return "OK"
}
