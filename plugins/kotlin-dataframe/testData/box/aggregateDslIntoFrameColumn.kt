// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val nested = dataFrameOf("number" to columnOf(1, null))
    val df = dataFrameOf(
        "key" to columnOf(0, 0),
        "nestedFrames" to columnOf(nested, nested),
    )

    val result = df.groupBy { key }.aggregate {
        nestedFrames into "aggregatedFrames"
    }

    result[0].let {
        checkExactType<Int?>(it.aggregatedFrames[0][0].number)
    }
    // Compile time has more info about aggregatedFrames type than runtime
    result.remove { aggregatedFrames }.compareSchemas(strict = true)

    return "OK"
}
