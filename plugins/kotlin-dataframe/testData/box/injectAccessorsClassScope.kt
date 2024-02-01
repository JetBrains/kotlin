package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

internal class ClassScope {
    val df = DataFrame.readJson("/home/nikita/IdeaProjects/dataframe-examples/datasets/achievements_all.json")

    fun use() {
        injectAccessors(df)
        df.originalId
    }
}

fun box(): String {
    ClassScope().use()
    return "OK"
}
