package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.add
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.convert
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.api.with

@DataSchema
interface Schema {
    val a: Int
}

fun main(args: Array<String>) {
    val res = dataFrameOf("a")(1)
        .cast<Schema>()
        .add("wwffffwwehirbwerffwffwffwfffffwfffwfwfwfaw") { 42 }

    res.wwffffwwehirbwerffwffwffwfffffwfffwfwfwfaw.print()
    res.a.print()

    val b = res.convert { a }.with { it.toString() }
    
    b.a

//    val res1 = res.conv
    //res.filter { it }
}
