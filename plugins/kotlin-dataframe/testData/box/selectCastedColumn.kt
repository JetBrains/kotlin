// CHECK_TYPE_WITH_EXACT

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
    ).cast<Any>()

    val rawProperties = df.select("name", "age", "city")

    rawProperties.select { name.cast<String>() }.let {
        checkExactType<String>(it[0].name)
    }

    rawProperties.mapToFrame {
        +name.cast<String>()
    }.let {
        it.compareSchemas(strict = true)
    }
    return "OK"
}
