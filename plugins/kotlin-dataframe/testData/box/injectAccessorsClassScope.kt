package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.api.*

internal class ClassScope {
    val df = DataFrame.readJsonDefault("/home/nikitak/IdeaProjects/dataframe-examples/datasets/achievements_all.json")

    fun use() {
        injectAccessors(df)
        df.originalId
    }
}

fun box(): String {
    ClassScope().use()
    return "OK"
}
