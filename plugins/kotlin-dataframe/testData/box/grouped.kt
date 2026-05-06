// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val grouped = dataFrameOf("a" to columnOf(1, 2, 3)).groupBy { a }.asGrouped()

    val df = grouped.maxFor { a named "b" }
    checkExactType<Int?>(df[0].b)
    df.compareSchemas()
    return "OK"
}

fun <T, G> GroupBy<T, G>.asGrouped(): Grouped<G> = this
