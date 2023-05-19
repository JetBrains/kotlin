package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.*

fun box(): String {
    test()
    return "OK"
}

fun test() {
    val df = dataFrameOf("a")(1)
    df.add("ff") { 42 }.print()
}
