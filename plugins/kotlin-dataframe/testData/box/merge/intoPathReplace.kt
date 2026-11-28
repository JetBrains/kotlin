// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "a" to columnOf(1, 2),
        "b" to columnOf(3, null),
        "c" to columnOf("x", "y"),
    )

    df.merge { a and b }.into(pathOf("b")).let {
        checkExactType<List<Int?>>(it.b[0])
        it.compareSchemas(strict = true)
    }

    df.group { a and b }.into("g").merge { g.a and g.b }.by { it.toString() }.into(pathOf("g", "a")).let {
        checkExactType<String>(it.g.a[0])
        it.compareSchemas(strict = true)
    }
    return "OK"
}
