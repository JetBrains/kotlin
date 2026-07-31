/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test

internal const val SMOKE_TEST_CONFIG_KEY = "org.jetbrains.kotlin.testFederation.smokeTestConfig"

/**
 * Whether test federation is enabled for this project.
 *
 * Test federation is typically enabled only in CI environments. Local test runs execute all tests by default unless test federation is
 * explicitly enabled through the corresponding Gradle property or environment variable.
 */
@DelicateTestFederationApi
val Project.testFederationEnabled: Boolean
    get() = providers.gradleProperty(TEST_FEDERATION_ENABLED_KEY).map { it.toBoolean() }
        .orElse(providers.environmentVariable(TEST_FEDERATION_ENABLED_ENV_KEY).map { it.toBoolean() })
        .getOrElse(false)


/**
 * Provides the [Domain]s to which this project belongs.
 */
@DelicateTestFederationApi
val Project.testFederationDomain: Provider<List<Domain>> by extensionProperty {
    project.provider { repositoryPath(this.projectDir.toPath()).domains }
}


/**
 * Provides the [TestFederationMode] assigned to this project.
 *
 * A project that belongs to at least one affected [Domain] uses [TestFederationMode.Full]. An unaffected project uses
 * [TestFederationMode.Smoke]. An explicitly configured mode takes precedence over the mode inferred from affected domains.
 *
 * If test federation is disabled, this provider always returns [TestFederationMode.Full]. Test federation is disabled by default for local,
 * non-CI development, so local test runs use the full mode unless test federation is explicitly enabled.
 */
@DelicateTestFederationApi
val Project.testFederationMode: Provider<TestFederationMode> by extensionProperty property@{
    if (!project.testFederationEnabled) {
        return@property provider { TestFederationMode.Full }
    }

    (providers.gradleProperty(TEST_FEDERATION_MODE_KEY)
        .orElse(providers.environmentVariable(TEST_FEDERATION_MODE_ENV_KEY)))
        .map(TestFederationMode::valueOf)
        .orElse(project.testFederationAffectedDomains.zip(testFederationDomain) { affectedTestSystems, domain ->
            if (domain.intersect(affectedTestSystems).isNotEmpty()) TestFederationMode.Full
            else TestFederationMode.Smoke
        })
}

/**
 * Provides the set of [Domain]s currently marked as affected.
 *
 * For example, a change to the Kotlin Gradle Plugin might affect [Domain.Gradle]. Explicitly configured affected domains take precedence over
 * domains inferred by the affected-domains service.
 *
 * If test federation is disabled, this provider contains every entry in [Domain.entries]. This is the default for local, non-CI development.
 */
@DelicateTestFederationApi
val Project.testFederationAffectedDomains: Provider<Set<Domain>> by extensionProperty property@{
    if (!project.testFederationEnabled) {
        return@property provider { Domain.entries.toSet() }
    }

    (providers.gradleProperty(TEST_FEDERATION_AFFECTED_DOMAINS_KEY)
        .orElse(providers.environmentVariable(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY)))
        .map { argumentString -> Domain.fromArgumentStringOrThrow(argumentString) }
        .orElse(project.affectedDomainsService.map { it.affectedDomains })
}

/**
 * Configures this test task's behavior in smoke mode.
 *
 * The default is [SmokeTestConfig.Default], which runs all tests annotated with `@SmokeTest` or `@AffectedBy`.
 *
 * **Disable this test task in smoke mode:**
 * ```kotlin
 * smokeTestConfig = SmokeTestConfig.Disabled
 * ```
 *
 * **Automatically select 3% of tests for the smoke test:**
 * ```kotlin
 * smokeTestConfig = SmokeTestConfig.Enabled(
 *     autoSmokeTestPercentage = 3
 * )
 * ```
 *
 * **Run the entire test task in smoke mode:**
 * ```kotlin
 * smokeTestConfig = SmokeTestConfig.RunAllTests
 * ```
 */
val Test.smokeTestConfig: Property<SmokeTestConfig> by extensionProperty {
    project.objects.property(SmokeTestConfig::class.java).convention(SmokeTestConfig.Default)
}

/**
 * Provides whether this project is tested in [TestFederationMode.Smoke].
 *
 * See `repo/TEST-FEDERATION.md` for details.
 */
val Project.isSmokeTestMode: Provider<Boolean> get() = testFederationMode.map { it == TestFederationMode.Smoke }


/**
 * Provides whether nightly tests are enabled for the current build.
 *
 * The value is `true` for nightly aggregates and `false` for non-nightly remote builds, such as master-based runs, regular aggregates, and
 * safe-merge builds. It defaults to `true` so that nightly tests can be run locally without additional configuration.
 */
val Project.areNightlyTestsEnabled: Provider<Boolean>
    get() = project.providers.gradleProperty("nightly").map { it.toBooleanStrict() }
        .orElse(providers.environmentVariable("NIGHTLY").map { it.toBooleanStrict() })
        .orElse(true)
