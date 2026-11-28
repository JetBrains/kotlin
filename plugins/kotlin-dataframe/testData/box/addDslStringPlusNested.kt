// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a" to columnOf("x"), "b" to columnOf(1))

    df.add {
        "g" {
            +"a"
        }
        group("h") {
            +"b"
        }
        group {
            +"a"
        } into "i"
    }.let {
        checkExactType<String>(it.g.a[0])
        checkExactType<Int>(it.h.b[0])
        checkExactType<String>(it.i.a[0])
        it.compareSchemas(strict = true)
    }
    return "OK"
}
