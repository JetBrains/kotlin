// FIR_DUMP

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("name", "age", "city", "weight")(
        "Alice", 15, "London", 54,
        "Bob", 45, "Dubai", 87,
        "Charlie", 20, "Moscow", null,
        "Charlie", 40, "Milan", null,
        "Bob", 30, "Tokyo", 68,
        "Alice", 20, null, 55,
        "Charlie", 30, "Moscow", 90,
    )

    val sorted1 = df.reorderColumnsByName()

    val sorted2 = df.groupBy { city }.into("a").reorderColumnsByName()

    val sorted3 = df.groupBy { city }.into("a").reorderColumnsByName(desc = true, atAnyDepth = false)

    val sorted4 = df.groupBy { city }.into("a").reorderColumnsByName(desc = true, atAnyDepth = true)
    return "OK"
}
