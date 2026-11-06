// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "age" to columnOf(15, 45, 20, 40),
        "weight" to columnOf(54, 87, 90, 68),
        "height" to columnOf(160, 175, 168, 180),
        "name" to columnOf("Alice", "Bob", "Charlie", "Dave"),
    )

    df.corr { age and weight }.withItself().let {
        checkExactType<String>(it.column[0])
        checkExactType<Double>(it.age[0])
        checkExactType<Double>(it.weight[0])
        it.compareSchemas(strict = true)
    }
    return "OK"
}
