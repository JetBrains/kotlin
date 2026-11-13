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

    df.merge { a and b }.into(pathOf("ab")).let {
        checkExactType<List<Int?>>(it.ab[0])
        it.compareSchemas(strict = true)
    }

    df.merge { a and b }.by { it.size }.into(pathOf("size")).let {
        checkExactType<Int>(it.size[0])
        it.compareSchemas(strict = true)
    }
    return "OK"
}
