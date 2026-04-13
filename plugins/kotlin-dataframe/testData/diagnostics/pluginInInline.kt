package org.jetbrains.kotlinx.dataframe.annotations

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*


@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FILE, AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION)
public annotation class DisableInterpretation

private inline fun <reified T> convert(l: List<T>) = l.<!DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_GENERIC, DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_INLINE!>toDataFrame<!>()

@DisableInterpretation
private inline fun <reified T> convert1(l: List<T>) = l.<!DATAFRAME_PLUGIN_IS_DISABLED!>toDataFrame<!>()


fun box(): String {
    convert(listOf(1, 2, 3))
    return "OK"
}
