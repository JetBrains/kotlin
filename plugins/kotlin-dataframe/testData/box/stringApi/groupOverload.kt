package org.jetbrains.kotlinx.dataframe.annotations

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.columns.toColumnSet

annotation class StringApiInterpretable(val interpreter: String, val stringArgument: String, val targetArgument: String = "")

@StringApiInterpretable(interpreter = "Group0", stringArgument = "columns", targetArgument = "columns")
public fun <T> DataFrame<T>.myGroup(vararg columns: String): GroupClause<T, Any?> = group { columns.toColumnSet() }

fun box(): String {
    dataFrameOf("c" to columnOf(42)).myGroup("c").into("gr").let { res ->
        val i: Int = res[0].gr.c
    }
    return "OK"
}
