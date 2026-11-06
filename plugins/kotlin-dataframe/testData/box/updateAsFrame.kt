// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "age" to columnOf(15, 45),
        "nameAndCity" to columnOf(
            "name" to columnOf("Alice", "Bob"),
            "city" to columnOf("London", null),
        ),
    )

    df.update { nameAndCity }.asFrame { it.remove { city } }.let {
        checkExactType<String>(it.nameAndCity.name[0])
        it.compareSchemas(strict = true)
    }
    return "OK"
}
