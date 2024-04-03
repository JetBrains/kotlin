package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.api.*

fun box(): String {
    val df = DataFrame.read("https://raw.githubusercontent.com/Kotlin/dataframe/master/data/jetbrains_repositories.csv")
    df.full_name
    return "OK"
}
