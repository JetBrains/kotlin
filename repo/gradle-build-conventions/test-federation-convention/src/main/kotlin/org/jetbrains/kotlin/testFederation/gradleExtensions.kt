/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.AbstractTestTask
import java.io.File

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
 * Beware: While a project might belong to the provided list of domains, individual test tasks can override the list
 * of domains.
 * @see AbstractTestTask.testFederationDomains
 */
@DelicateTestFederationApi
val Project.testFederationDomains: Provider<List<Domain>> by extensionProperty {
    project.provider { repositoryPath(this.projectDir.toPath()).domains }
}

/**
 * Provides the [Domain]s to which this Test task belongs to.
 * This can be overridden.
 *
 * **example**: Make a test task belong to the 'Js' and 'Wasm' domains
 * ```kotlin
 * testTask {
 *     testFederationDomains = listOf(Domain.Js, Domain.Wasm)
 * }
 * ```
 */
@DelicateTestFederationApi
val AbstractTestTask.testFederationDomains: ListProperty<Domain> by extensionProperty {
    project.objects.listProperty(Domain::class.java).value(project.testFederationDomains)
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
val AbstractTestTask.testFederationMode: Provider<TestFederationMode> by extensionProperty property@{
    project.provider {
        /* Disabled Test Federation -> Always run in 'Full' Mode */
        if (!project.testFederationEnabled) {
            return@provider TestFederationMode.Full
        }

        /* Always run in 'Full' mode by configuration */
        if (smokeTestConfig.get() == SmokeTestConfig.RunAllTests) {
            return@provider TestFederationMode.Full
        }

        /* External override by gradle property or environment variable shall be respected */
        project.providers.gradleProperty(TEST_FEDERATION_MODE_KEY)
            .orElse(project.providers.environmentVariable(TEST_FEDERATION_MODE_ENV_KEY))
            .map(TestFederationMode::valueOf)
            .orNull?.let { return@provider it }

        null
    }.orElse(
        testFederationDomains.zip(project.testFederationAffectedDomains) { testFederationDomains, testFederationAffectedDomains ->
            if (testFederationDomains.intersect(testFederationAffectedDomains).isNotEmpty()) TestFederationMode.Full
            else TestFederationMode.Smoke
        }
    )
}

/**
 * Provides a list of file-paths which are marked by the test federation to be changed.
 *
 * If the test federation is disabled, the returned list will always be empty
 */
@DelicateTestFederationApi
val Project.testFederationChangedFiles: Provider<List<String>> by extensionProperty property@{
    if (!testFederationEnabled) return@property provider { emptyList() }
    providers.gradleProperty(TEST_FEDERATION_CHANGED_FILES_KEY).map { raw -> raw.split(File.pathSeparatorChar) }
        .orElse(featureBranchDiffService.map { it.diff })
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

    /* Handle the case where only -Ptest.federation.changed.domains is provided, but affected domains are not */
    val fromProvidedChangedDomains = (providers.gradleProperty(TEST_FEDERATION_CHANGED_DOMAINS_KEY))
        .orElse(providers.environmentVariable(TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY))
        .map { raw -> Domain.fromArgumentStringOrThrow(raw).withAffectedDependencies() }

    (providers.gradleProperty(TEST_FEDERATION_AFFECTED_DOMAINS_KEY)
        .orElse(providers.environmentVariable(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY)))
        .map { argumentString -> Domain.fromArgumentStringOrThrow(argumentString) }
        .orElse(fromProvidedChangedDomains)
        .orElse(project.affectedDomainsService.map { it.affectedDomains })
}

@DelicateTestFederationApi
val Project.testFederationChangedDomains: Provider<Set<Domain>> by extensionProperty property@{
    if (!project.testFederationEnabled) {
        return@property provider { Domain.entries.toSet() }
    }

    (providers.gradleProperty(TEST_FEDERATION_CHANGED_DOMAINS_KEY)
        .orElse(providers.environmentVariable(TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY))
        .orElse(providers.gradleProperty(TEST_FEDERATION_AFFECTED_DOMAINS_KEY))
        .orElse(providers.environmentVariable(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY)))
        .map { argumentString -> Domain.fromArgumentStringOrThrow(argumentString) }
        .orElse(project.affectedDomainsService.map { it.changedDomains })
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
val AbstractTestTask.smokeTestConfig: Property<SmokeTestConfig> by extensionProperty {
    project.objects.property(SmokeTestConfig::class.java).convention(SmokeTestConfig.Default)
}

/**
 * Provides whether this project is tested in [TestFederationMode.Smoke].
 *
 * See `repo/TEST-FEDERATION.md` for details.
 */
val AbstractTestTask.isSmokeTestMode: Provider<Boolean> get() = testFederationMode.map { it == TestFederationMode.Smoke }


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
