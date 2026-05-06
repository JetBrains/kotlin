// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import kotlin.reflect.typeOf

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

    df.groupBy { city.map { it?.uppercase() } }.toDataFrame().let { aggregated ->
        aggregated.compareSchemas(strict = true)
    }

    df.groupBy { city.map { it?.uppercase() ?: "" } named "cityU" }.toDataFrame().let { aggregated ->
        aggregated.compareSchemas(strict = true)
    }

    dataFrameOf("a" to columnOf(42)).convert { a.map { (it * 2).toString() } }.with { it.toInt().toDouble() }.let {
        it.compareSchemas(strict = true)
        it.print()
    }

    df.groupBy { city.mapIndexed { _, it -> it?.uppercase() } }.toDataFrame().let { aggregated ->
        aggregated.compareSchemas(strict = true)
    }

    fun String?.myUppercase(): Any? = this?.uppercase()

    df.groupBy { city.map(type = typeOf<String?>()) { it?.myUppercase() } }.toDataFrame().let { aggregated ->
        checkExactType<Any?>(aggregated[0].city)
        aggregated.assert()
    }

    df.groupBy { city.mapIndexed(type = typeOf<String?>()) { _, it -> it?.myUppercase() } }.toDataFrame().let { aggregated ->
        checkExactType<Any?>(aggregated[0].city)
        aggregated.assert()
    }
    return "OK"
}
