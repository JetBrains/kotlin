package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.*

@DataSchema
interface A {
    val a: Int
}

@DataSchema
interface B {
    val b: Int
    val b1: Int
}

fun check(df: DataFrame<A>) {
    df.<!CAST_ERROR!>cast<!><B>()
}

fun checkAny(df: DataFrame<*>) {
    df.cast<B>()
}

fun checkEmptySchema() {
    dataFrameOf(("a" + "b") to columnOf(123)).cast<A>()
}