/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.util

internal inline fun <reified T> flatListOf(vararg lists: List<T>): List<T> {
    val overallSize = lists.sumBy { it.size }
    val aggregatingList = ArrayList<T>(overallSize)
    lists.forEach {
        aggregatingList.addAll(it)
    }
    return aggregatingList
}

internal inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> {
    return this.map(transform).toSet()
}
