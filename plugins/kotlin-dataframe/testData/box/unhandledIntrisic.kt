package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun main() {
    val df = DataFrame.readCSV("https://raw.githubusercontent.com/Kotlin/dataframe/master/data/jetbrains_repositories.csv")
    val df1 = df
        .add("topicsList") { topics.removeSurrounding("[", "]").split(", ").filter { it.isNotEmpty() } }
}
