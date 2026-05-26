/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

internal const val TEST_FEDERATION_ENABLED_KEY = "test.federation.enabled"
internal const val TEST_FEDERATION_ENABLED_ENV_KEY = "TEST_FEDERATION_ENABLED"
internal const val TEST_FEDERATION_MODE_KEY = "test.federation.mode"
internal const val TEST_FEDERATION_MODE_ENV_KEY = "TEST_FEDERATION_MODE"
internal const val TEST_FEDERATION_AFFECTED_DOMAINS_KEY = "test.federation.affected.domains"
internal const val TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY = "TEST_FEDERATION_AFFECTED_DOMAINS"
internal const val TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY = "test.federation.auto.smoke.test.percentage"
internal const val TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY = "TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE"
const val TEST_FEDERATION_NIGHTLY_KEY = "test.federation.nightly"
const val TEST_FEDERATION_NIGHTLY_ENV_KEY = "TEST_FEDERATION_NIGHTLY"

/**
 * @return true: If the test federation is enabled (typically only on CI environments)
 * false: Locally: All tests will be executed.
 */
val testFederationEnabled: Boolean
    get() = resolve(TEST_FEDERATION_ENABLED_KEY, TEST_FEDERATION_ENABLED_ENV_KEY)?.toBoolean() ?: false

/**
 * @return the current [TestFederationMode]. Only relevant if the [testFederationEnabled] returns true
 */
val testFederationMode: TestFederationMode?
    get() {
        val raw = resolve(TEST_FEDERATION_MODE_KEY, TEST_FEDERATION_MODE_ENV_KEY) ?: return null
        return TestFederationMode.valueOf(raw)
    }

/**
 * @return All affected [Domain]s. Only relevant if the [testFederationEnabled] returns true
 */
val testFederationAffectedDomains: Set<Domain>?
    get() {
        val raw = resolve(TEST_FEDERATION_AFFECTED_DOMAINS_KEY, TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY) ?: return null
        if (raw.isBlank()) return null
        return raw.split(";").flatMap { value ->
            when (value) {
                "*" -> Domain.entries
                "<none>" -> emptyList()
                else -> listOf(Domain.valueOf(value))
            }
        }.sorted().toSet()
    }

/**
 * Tests marked with `@NightlyTest` are considered 'nightly tests'. Those tests shall not be executed
 * during the master aggregate, but only on nightly CI runs. Nightlies are typically enabled for local
 * development flows.
 * @return 'true' if nightly tests are enabled, 'false' if nightly tests shall be skipped.
 */
val testFederationNightly: Boolean by lazy {
    resolve(TEST_FEDERATION_NIGHTLY_KEY, TEST_FEDERATION_NIGHTLY_ENV_KEY)?.toBoolean() ?: false
}

internal val autoSmokeTestPercentage: Int = run {
    resolve(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY, TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY)?.toInt() ?: -1
}

private fun resolve(key: String, envKey: String): String? =
    System.getProperty(key) ?: System.getenv(envKey)
