package org.jetbrains.kotlinx.dataframe.annotations

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet

annotation class StringApiInterpretable(val interpreter: String, val stringArgument: String, val targetArgument: String = "")

@Refine
@StringApiInterpretable(interpreter = "Flatten0", stringArgument = "columns", targetArgument = "columns")
public fun <T> DataFrame<T>.myFlatten(
    vararg columns: String,
    keepParentNameForColumns: Boolean = false,
    separator: String = "_",
): DataFrame<T> = @DisableInterpretation flatten(keepParentNameForColumns, separator) { columns.toColumnSet() }

fun box(): String {
    val res = dataFrameOf(
        "a" to columnOf(
            "b" to columnOf("str")
        )
    ).myFlatten("a")
    val v: String = res.b[0]

    return "OK"
}
