/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

fun Iterable<TestCluster>.toArgumentString(): String =
    joinToString(",") { it.name }

fun String.toTestClusters(): Set<TestCluster> =
    if (isBlank()) emptySet() else split(",").map { TestCluster.valueOf(it.trim()) }.toSet()
