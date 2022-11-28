package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.api.asGroupBy
import org.jetbrains.kotlinx.dataframe.api.convert
import org.jetbrains.kotlinx.dataframe.api.count
import org.jetbrains.kotlinx.dataframe.api.explode
import org.jetbrains.kotlinx.dataframe.api.filter
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.joinDefault
import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.api.readJsonDefault
import org.jetbrains.kotlinx.dataframe.api.select
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.api.sortByDesc
import org.jetbrains.kotlinx.dataframe.api.sum
import org.jetbrains.kotlinx.dataframe.api.sumOf
import org.jetbrains.kotlinx.dataframe.api.with

fun box(): String {
    val df = DataFrame.readJsonDefault("/home/nikitak/IdeaProjects/dataframe-examples/datasets/achievements_all.json")

    val df2 = df.explode { achievements }

    df2
        .filter { achievements.preStage != null }
        .joinDefault(df2) { achievements.preStage.match(right.achievements.id) }
    return "OK"
}
