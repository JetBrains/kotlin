package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.*

fun box(): String {
    test()
    return "OK"
}

fun test() {
    val df = dataFrameOf("a")(1)
    df.add("col1") { 42 }.col1.print()
}
