package org.jetbrains.kotlinx.dataframe.api

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

//@DataSchema
//interface Cars {
//    val year: Int
//}

interface Cars

fun `Name is evaluated to age`(df: DataFrame<Cars>) {
    val df1 = df.add("age") { 2022 }
    val col = df1.age
}
