package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.api.*

fun main() {
    val df = DataFrame.readCSVDefault("https://raw.githubusercontent.com/Kotlin/dataframe/master/data/jetbrains_repositories.csv")
    val df1 = df
        .add("topicsList") { topics.removeSurrounding("[", "]").split(", ").filter { it.isNotEmpty() } }
}
