// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "achievements" to columnOf(
            dataFrameOf("order" to columnOf(1, 2)),
            dataFrameOf("order" to columnOf(3, 4)),
        ),
    )

    df.split { achievements }.inward("a", "b").let {
        checkExactType<Int>(it.achievements.a.order[0])
        checkExactType<Int>(it.achievements.b.order[0])
        it.compareSchemas(strict = true)
    }
    return "OK"
}
