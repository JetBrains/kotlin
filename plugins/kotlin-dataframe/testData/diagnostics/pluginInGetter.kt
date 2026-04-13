package org.jetbrains.kotlinx.dataframe.annotations

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FILE, AnnotationTarget.EXPRESSION, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class DisableInterpretation

class Context {
    val peopleDf: DataFrame<Any?> get() = <!DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR!>dataFrameOf<!>("firstName" to columnOf("Alice"))

    @DisableInterpretation
    val peopleDf1: DataFrame<Any?> get() = <!DATAFRAME_PLUGIN_IS_DISABLED!>dataFrameOf<!>("firstName" to columnOf("Alice"))
}