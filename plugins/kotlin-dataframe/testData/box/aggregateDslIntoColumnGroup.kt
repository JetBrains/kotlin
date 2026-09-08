// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import kotlin.reflect.typeOf

fun box(): String {
    val df = dataFrameOf(
        "key" to columnOf(0, 0),
        "details" to columnOf(
            "number" to columnOf(1, null)
        ),
    )

    val result = df.groupBy { key }.aggregate {
        details into "aggregatedDetails"
    }

    result[0].let {
        checkExactType<Int?>(it.aggregatedDetails[0].number)
    }
    result.compareSchemas(strict = true)
    return "OK"
}
