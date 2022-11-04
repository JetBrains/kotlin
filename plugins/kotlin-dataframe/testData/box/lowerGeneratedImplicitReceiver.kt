package org.jetbrains.kotlinx.dataframe.api

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

interface Cars

fun box(): String {
    val df = dataFrameOf("a")(1).cast<Cars>()
    val df1 = df.add("age") { 2022 }
    val col = df1.age
    return if (col[0] == 2022) "OK" else col[0].toString()
}
