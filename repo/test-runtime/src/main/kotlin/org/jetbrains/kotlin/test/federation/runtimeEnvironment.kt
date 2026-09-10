/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

internal const val TEST_FEDERATION_ENABLED_KEY = "test.federation.enabled"
internal const val TEST_FEDERATION_ENABLED_ENV_KEY = "TEST_FEDERATION_ENABLED"
internal const val TEST_FEDERATION_MODE_KEY = "test.federation.mode"
internal const val TEST_FEDERATION_MODE_ENV_KEY = "TEST_FEDERATION_MODE"
internal const val TEST_FEDERATION_CHANGED_DOMAINS_KEY = "test.federation.changed.domains"
internal const val TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY = "TEST_FEDERATION_CHANGED_DOMAINS"
internal const val TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY = "test.federation.auto.smoke.test.percentage"
internal const val TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY = "TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE"
const val TEST_FEDERATION_NIGHTLY_KEY = "test.federation.nightly"
const val TEST_FEDERATION_NIGHTLY_ENV_KEY = "TEST_FEDERATION_NIGHTLY"

/**
 * Reports whether Test Federation is enabled in the runtime configuration. Defaults to `false`.
 * The execution condition uses [testFederationMode] to select tests, not this flag directly.
 */
val testFederationEnabled: Boolean =
    resolve(TEST_FEDERATION_ENABLED_KEY, TEST_FEDERATION_ENABLED_ENV_KEY)?.toBoolean() ?: false

/**
 * Provides the configured test selection mode, or `null` when no mode is configured.
 * With no mode, the execution condition does not restrict test selection. Other test filters still apply.
 */
val testFederationMode: TestFederationMode? = run {
    val raw = resolve(TEST_FEDERATION_MODE_KEY, TEST_FEDERATION_MODE_ENV_KEY) ?: return@run null
    TestFederationMode.valueOf(raw)
}

/**
 * Provides the configured domains containing changed files, or `null` when the value is absent or blank.
 * Used to select tests marked to run for changes in those domains when no full run is selected.
 */
val testFederationChangedDomains: Set<Domain>? = run {
    val raw = resolve(TEST_FEDERATION_CHANGED_DOMAINS_KEY, TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY) ?: return@run null
    if (raw.isBlank()) return@run null
    domainsFromString(raw)
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

private fun domainsFromString(value: String): Set<Domain> {
    return value.split(";").flatMap { value ->
        when (value) {
            "*" -> Domain.entries
            "<none>" -> emptyList()
            else -> listOf(Domain.valueOf(value))
        }
    }.sorted().toSet()
}
