/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

internal const val TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY = "test.federation.auto.smoke.test.percentage"
internal const val TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY = "TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE"
internal const val TEST_FEDERATION_CLUSTERS_KEY = "test.federation.clusters"
internal const val TEST_FEDERATION_CLUSTERS_ENV_KEY = "TEST_FEDERATION_CLUSTERS"
const val TEST_FEDERATION_NIGHTLY_KEY = "test.federation.nightly"
const val TEST_FEDERATION_NIGHTLY_ENV_KEY = "TEST_FEDERATION_NIGHTLY"

/**
 * Provides the configured test clusters, or `null` when none are configured.
 * With no clusters configured, the discovery filter does not restrict test selection. Other test filters still apply.
 */
internal val testFederationClusters: Set<TestCluster>? =
    resolve(TEST_FEDERATION_CLUSTERS_KEY, TEST_FEDERATION_CLUSTERS_ENV_KEY)?.toTestClusters()

private fun String.toTestClusters(): Set<TestCluster> =
    if (isBlank()) emptySet() else split(",").map { TestCluster.valueOf(it.trim()) }.toSet()


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
