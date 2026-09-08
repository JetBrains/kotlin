// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val nested = dataFrameOf("number" to columnOf(1, null))
    val df = dataFrameOf(
        "numbers" to columnOf(1, null),
        "details" to columnOf("number" to columnOf(1, null)),
        "nestedFrames" to columnOf(nested, nested),
    )

    val result = df.aggregate {
        numbers into "aggregatedNumbers"
        details into "aggregatedDetails"
        nestedFrames into "aggregatedFrames"
    }

    checkExactType<List<Int?>>(result.aggregatedNumbers)
    checkExactType<Int?>(result.aggregatedDetails[0].number)
    checkExactType<Int?>(result.aggregatedFrames[0][0].number)
    // Compile time has more info about aggregatedFrames type than runtime
    result.toDataFrame().remove { aggregatedFrames }.compareSchemas(strict = true)

    return "OK"
}
