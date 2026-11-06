// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    listOf(1, 2, 3).toDataFrame {
        expr { it + 1 } into "inc"
    }.let {
        checkExactType<Int>(it.inc[0])
        it.compareSchemas(strict = true)
    }
    return "OK"
}
