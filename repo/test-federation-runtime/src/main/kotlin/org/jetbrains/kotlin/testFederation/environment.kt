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

private fun resolve(key: String, envKey: String): String? =
    System.getProperty(key) ?: System.getenv(envKey)
