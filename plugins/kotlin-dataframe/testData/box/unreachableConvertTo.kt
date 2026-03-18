// WITH_EXTRA_CHECKERS

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("group" to columnOf(
        "a" to columnOf(1),
    ))

    try {
        df.convert { group }.toInt().group[0]
    } catch (e: Exception) { }
    return "OK"
}
