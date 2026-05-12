/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test

/**
 * The test federation is typically only enabled in CI environments.
 * Running tests locally will always execute all tests unless actively opted into the test federation.
 */
internal val Project.testFederationEnabled: Boolean
    get() = providers.gradleProperty(TEST_FEDERATION_ENABLED_KEY).map { it.toBoolean() }
        .orElse(providers.environmentVariable(TEST_FEDERATION_ENABLED_ENV_KEY).map { it.toBoolean() })
        .getOrElse(false)


/**
 * Returns the [Domain] the current project belongs to
 */
internal val Project.testFederationDomain: Provider<Domain>
    get() = project.provider { repositoryPath(this.projectDir.toPath()).domain }

internal val Project.testFederationMode: Provider<TestFederationMode>
    get() {
        if (!project.testFederationEnabled) {
            return provider { TestFederationMode.Full }
        }

        return providers.environmentVariable(TEST_FEDERATION_MODE_ENV_KEY).map(TestFederationMode::valueOf)
            .orElse(project.testFederationAffectedDomains.zip(testFederationDomain) { affectedTestSystems, domain ->
                if (domain in affectedTestSystems) TestFederationMode.Full
                else TestFederationMode.Smoke
            })
    }

internal val Project.testFederationAffectedDomains: Provider<Set<Domain>>
    get() {
        if (!project.testFederationEnabled) {
            return provider { Domain.entries.toSet() }
        }

        return providers.environmentVariable(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY)
            .map { argumentString -> inferAffectedDomains(argumentString) }
            .orElse(project.affectedDomainsService.map { it.affectedDomains })
    }


internal const val SMOKE_TEST_CONFIG_KEY = "org.jetbrains.kotlin.testFederation.smokeTestConfig"

@TemporaryTestFederationApi
var Test.smokeTestConfig: SmokeTestConfig?
    set(value) = extensions.extraProperties.set(SMOKE_TEST_CONFIG_KEY, value)
    get() = if (extensions.extraProperties.has(SMOKE_TEST_CONFIG_KEY)) extensions.extraProperties.get(SMOKE_TEST_CONFIG_KEY) as SmokeTestConfig? else null
