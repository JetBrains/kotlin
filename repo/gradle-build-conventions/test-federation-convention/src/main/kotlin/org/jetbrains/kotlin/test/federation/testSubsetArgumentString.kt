/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

fun Iterable<TestSubset>.toArgumentString(): String {
    return toSet().sorted().joinToString(",") { it.name }
}

fun String.toTestSubsets(): Set<TestSubset> {
    val trimmed = trim()
    return when {
        trimmed.isBlank() -> emptySet()
        else -> trimmed.split(",").map { TestSubset.valueOf(it.trim()) }.toSet()
    }
}
