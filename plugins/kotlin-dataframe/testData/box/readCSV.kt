package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = DataFrame.readCSV("https://raw.githubusercontent.com/Kotlin/dataframe/master/data/jetbrains_repositories.csv")
    df.full_name.print()
    return "OK"
}
