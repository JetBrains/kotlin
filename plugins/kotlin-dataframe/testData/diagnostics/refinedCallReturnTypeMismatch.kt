package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.*

// KDF#1490: a type mismatch of a refined call has to be reported on the call instead of crashing the checker

@DataSchema
interface Iso6393Raw

fun test(): DataFrame<Iso6393Raw> {
    return <!RETURN_TYPE_MISMATCH!>dataFrameOf("a" to columnOf(1)).cast<Iso6393Raw>().add("b") { 2 }<!>
}

fun testExpressionBody(): DataFrame<Iso6393Raw> = <!RETURN_TYPE_MISMATCH!>dataFrameOf("a" to columnOf(1)).cast<Iso6393Raw>().add("b") { 2 }<!>

fun testWithVariable(): DataFrame<Iso6393Raw> {
    val df = dataFrameOf("a" to columnOf(1)).cast<Iso6393Raw>().add("b") { 2 }
    return <!RETURN_TYPE_MISMATCH!>df<!>
}
