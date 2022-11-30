package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.cast

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
    <!CAST_ERROR!>df.cast<B>()<!>
}

fun checkAny(df: DataFrame<*>) {
    df.cast<B>()
}
