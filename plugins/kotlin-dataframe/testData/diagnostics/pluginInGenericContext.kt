package org.jetbrains.kotlinx.dataframe.annotations

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FILE, AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION)
public annotation class DisableInterpretation

@DisableInterpretation
private fun <T> create(valuesList: List<T>) = <!DATAFRAME_PLUGIN_IS_DISABLED!>dataFrameOf<!>("x" to valuesList)

private fun <T> create1(valuesList: List<T>) = <!DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_GENERIC!>dataFrameOf<!>("x" to valuesList)

class Container<T> {
    private fun test() = <!DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_GENERIC!>dataFrameOf<!>("a" to columnOf(1))
}

fun box(): String {
    val df = create(listOf(1, 2, 3))
    return "OK"
}
