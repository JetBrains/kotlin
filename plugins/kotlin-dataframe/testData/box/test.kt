package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

annotation class Gen

interface Cars

//@Gen
class PropertiesScope {
    val ColumnsContainer<Cars>.age: DataColumn<Int> get() = this["age"] <!UNCHECKED_CAST!>as DataColumn<Int><!>
}

@Gen
class PropertiesScope1 /*{
    val ColumnsContainer<Cars>.age: DataColumn<Int> get() = this["age"] as DataColumn<Int>
}*/

fun test(df: DataFrame<Cars>) {
    with(PropertiesScope()) {
        val col: DataColumn<Int> = df.age
    }
}

fun test1(df: DataFrame<Cars>) {
    with(PropertiesScope1()) {
        val col: DataColumn<Int> = df.age
    }
}
