@file:DisableInterpretation

package org.jetbrains.kotlinx.dataframe.annotations

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FILE, AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION)
public annotation class DisableInterpretation

fun box(): String {
    val df = <!DATAFRAME_PLUGIN_IS_DISABLED!>dataFrameOf<!>("a" to columnOf(1))
    return "OK"
}
