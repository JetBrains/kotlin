package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.api.asGroupBy
import org.jetbrains.kotlinx.dataframe.api.count
import org.jetbrains.kotlinx.dataframe.api.explode
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.api.select
import org.jetbrains.kotlinx.dataframe.api.sum
import org.jetbrains.kotlinx.dataframe.api.sumOf
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = DataFrame.readJson("/home/nikita/IdeaProjects/dataframe-examples/datasets/achievements_all.json")

    val df1 = df.explode { achievements }
    df1.achievements.order
    return "OK"
}
