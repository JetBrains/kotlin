// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "a" to columnOf(1, 2, 3),
        "b" to columnOf(2.0, 3.5, 1.0),
        "flag" to columnOf(true, false, true),
        "maybe" to columnOf(1, null, 3),
        "s" to columnOf("x", "y", "z"),
    )

    df.corr().let {
        checkExactType<String>(it.column[0])
        checkExactType<Double>(it.a[0])
        checkExactType<Double>(it.b[0])
        checkExactType<Double>(it.flag[0])
        it.compareSchemas(strict = true)
    }
    return "OK"
}
