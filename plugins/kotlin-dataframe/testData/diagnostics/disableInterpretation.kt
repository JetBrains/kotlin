package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DisableInterpretation
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.api.*

fun box(): String {
    // file doesn't exists, so analysis would fail should interpretation happen
    val df = @DisableInterpretation DataFrame.read("wowah_data_100K.csv")
    return "OK"
}
