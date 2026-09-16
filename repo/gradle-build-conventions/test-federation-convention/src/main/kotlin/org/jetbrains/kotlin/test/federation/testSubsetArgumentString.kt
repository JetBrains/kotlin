/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

fun Iterable<TestSubset>.toArgumentString(): String =
    joinToString(",") { it.toArgumentToken() }

fun String.toTestSubsets(): Set<TestSubset> =
    if (isBlank()) emptySet() else split(",").map { it.trim().toTestSubset() }.toSet()

private fun TestSubset.toArgumentToken(): String = when (this) {
    TestSubset.AllTests -> "all"
    TestSubset.SmokeTests -> "smoke"
    else -> "contract:${name.removePrefix("ContractTestsFor")}"
}

private fun String.toTestSubset(): TestSubset = when {
    this == "all" -> TestSubset.AllTests
    this == "smoke" -> TestSubset.SmokeTests
    startsWith("contract:") -> TestSubset.valueOf("ContractTestsFor${removePrefix("contract:")}")
    else -> error("Unknown TestSubset token: '$this'")
}
