/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

internal const val TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY = "test.federation.auto.smoke.test.percentage"
internal const val TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY = "TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE"
internal const val TEST_FEDERATION_SUBSETS_KEY = "test.federation.subsets"
internal const val TEST_FEDERATION_SUBSETS_ENV_KEY = "TEST_FEDERATION_SUBSETS"
const val TEST_FEDERATION_NIGHTLY_KEY = "test.federation.nightly"
const val TEST_FEDERATION_NIGHTLY_ENV_KEY = "TEST_FEDERATION_NIGHTLY"

private const val ALL_SUBSETS_NOTATION = "*"

/**
 * Whether the current run should exercise the full exhaustive variant set for parameterized, repeated,
 * test-template, and test-factory methods. When `false`, only the minimal variant (e.g. a single
 * Gradle/JDK version) is used to keep pre-merge smoke/contract runs fast.
 *
 * This is `true` whenever [TestSubset.PlainTests] is among the requested subsets, because `PlainTests`
 * represents a complete, non-smoke, non-contract run where full coverage is expected.
 */
val testFederationExhaustive: Boolean
    get() = TestSubset.PlainTests in testFederationSubsets

/**
 * Provides the configured test subsets. Defaults to every subset (`*`) when [TEST_FEDERATION_SUBSETS_KEY]
 * is not explicitly configured.
 */
internal val testFederationSubsets: Set<TestSubset> =
    (resolve(TEST_FEDERATION_SUBSETS_KEY, TEST_FEDERATION_SUBSETS_ENV_KEY) ?: ALL_SUBSETS_NOTATION).toTestSubsets()

private fun String.toTestSubsets(): Set<TestSubset> {
    val trimmed = trim()
    return when {
        trimmed.isBlank() -> emptySet()
        trimmed == ALL_SUBSETS_NOTATION -> TestSubset.entries.toSet()
        else -> trimmed.split(",").map { TestSubset.valueOf(it.trim()) }.toSet()
    }
}

/**
 * Reports whether nightly tests are enabled in the runtime configuration. Defaults to `false` when not configured.
 * The Gradle convention supplies this value and separately excludes nightly tags when nightly tests are disabled.
 * Local Gradle runs enable nightly tests by default; other test filters still apply.
 */
val testFederationNightly: Boolean = run {
    resolve(TEST_FEDERATION_NIGHTLY_KEY, TEST_FEDERATION_NIGHTLY_ENV_KEY)?.toBoolean() ?: false
}

internal val autoSmokeTestPercentage: Int = run {
    resolve(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY, TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY)?.toInt() ?: -1
}

private fun resolve(key: String, envKey: String): String? =
    System.getProperty(key) ?: System.getenv(envKey)
