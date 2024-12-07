/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.sources.android

fun <T> Set<T>.generatePairs(): Sequence<Pair<T, T>> {
    val values = this.toList()
    return sequence {
        for (index in values.indices) {
            val first = values[index]
            for (remainingIndex in (index + 1)..values.lastIndex) {
                val second = values[remainingIndex]
                yield(first to second)
            }
        }
    }
}
