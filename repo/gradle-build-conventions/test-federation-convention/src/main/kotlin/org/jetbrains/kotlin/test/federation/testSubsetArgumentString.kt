/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

private const val ALL_SUBSETS_NOTATION = "*"

fun Iterable<TestSubset>.toArgumentString(): String {
    val set = toSet()
    if (set.containsAll(TestSubset.entries)) return ALL_SUBSETS_NOTATION
    return set.sorted().joinToString(",") { it.name }
}

fun String.toTestSubsets(): Set<TestSubset> {
    val trimmed = trim()
    return when {
        trimmed.isBlank() -> emptySet()
        trimmed == ALL_SUBSETS_NOTATION -> TestSubset.entries.toSet()
        else -> trimmed.split(",").map { TestSubset.valueOf(it.trim()) }.toSet()
    }
}
