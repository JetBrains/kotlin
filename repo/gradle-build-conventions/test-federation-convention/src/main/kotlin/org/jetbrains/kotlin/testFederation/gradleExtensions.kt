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
internal val Project.testFederationEnabled: Provider<Boolean>
    get() = providers.gradleProperty(TEST_FEDERATION_ENABLED_KEY).map { it.toBoolean() }
        .orElse(providers.environmentVariable(TEST_FEDERATION_ENABLED_ENV_KEY).map { it.toBoolean() })


/**
 * Returns the [Domain] the current project belongs to
 */
internal val Project.testFederationDomain: Provider<Domain>
    get() = project.provider { repositoryPath(this.projectDir.toPath()).domain }

internal val Project.testFederationMode: Provider<TestFederationMode>
    get() = project.testFederationAffectedDomains.zip(testFederationDomain) { affectedTestSystems, domain ->
        if (domain in affectedTestSystems) TestFederationMode.Full
        else TestFederationMode.Smoke
    }

internal val Project.testFederationAffectedDomains: Provider<Set<Domain>>
    get() {
        return providers.environmentVariable(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY)
            .map { argumentString -> inferAffectedDomains(argumentString) }
            .orElse(project.affectedDomainsService.map { it.affectedDomains })
    }


private const val IS_SMOKE_TEST_KEY = "org.jetbrains.kotlin.testFederation.isSmokeTest"

/**
 * `true`: Marks the entire test task as 'smoke test'. All tests will always run
 * `false`: Marks the entire test task as 'not smoke test'; Never runs in smoke test mode
 * `null`: Default value; The test can be executed in regular (full) and smoke test modes
 */
@TemporaryTestFederationApi
var Test.isSmokeTest: Boolean?
    set(value) = extensions.extraProperties.set(IS_SMOKE_TEST_KEY, value)
    get() = if (extensions.extraProperties.has(IS_SMOKE_TEST_KEY)) extensions.extraProperties.get(IS_SMOKE_TEST_KEY) as Boolean else null