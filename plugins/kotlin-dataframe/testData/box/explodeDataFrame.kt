package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.api.asGroupBy
import org.jetbrains.kotlinx.dataframe.api.count
import org.jetbrains.kotlinx.dataframe.api.explode
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.api.readJsonDefault
import org.jetbrains.kotlinx.dataframe.api.select
import org.jetbrains.kotlinx.dataframe.api.sum
import org.jetbrains.kotlinx.dataframe.api.sumOf

fun box(): String {
    val df = DataFrame.readJsonDefault("/home/nikitak/IdeaProjects/dataframe-examples/datasets/achievements_all.json")

    val df1 = df.explode { achievements }
    return "OK"
}
