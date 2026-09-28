/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.process.CommandLineArgumentProvider
import javax.inject.Inject

abstract class TestFederationTaskExtension @Inject constructor() : CommandLineArgumentProvider {
    @get:Nested
    abstract val smokeTests: SmokeTestsSelection

    @get:Nested
    abstract val contractTests: ContractTestsSelection

    /**
     * Configure additional filters for the `SmokeTests` subset.
     *
     * Useful for auto-sampling smoke tests, as well as for JUnit3 tests that don't support `@MustRunAlways` annotations.
     *
     * Example:
     * ```kotlin
     * testFederation {
     *     smokeTests {
     *         // Sample 5% of all tests and add them to the SmokeTest subset
     *         // (requires JUnit5)
     *         includeAutoSamples(percentage = 5)
     *
     *         // Execute all tests when SmokeTests subset is requested
     *         // This makes SmokeTests equivalent to requesting every subset (*)
     *         includeAll()
     *
     *         // If only SmokeTests subset was requested, skip the task
     *         skip()
     *     }
     * }
     * ```
     */
    fun smokeTests(configure: SmokeTestsSelection.() -> Unit) {
        smokeTests.configure()
    }

    /**
     * Configure the `ContractTestsFor<Domain>` subsets.
     *
     * Example:
     * ```kotlin
     * testFederation {
     *     contractTests {
     *         // If only ContractTests* subsets were requested, skip the task
     *         skip()
     *     }
     * }
     * ```
     */
    fun contractTests(configure: ContractTestsSelection.() -> Unit) {
        contractTests.configure()
    }

    override fun asArguments(): Iterable<String> = emptyList()
}

abstract class SmokeTestsSelection {
    @get:Input
    @get:Optional
    abstract val autoSamplePercentage: Property<Int>

    @get:Input
    abstract val includeAll: Property<Boolean>

    @get:Input
    abstract val skip: Property<Boolean>

    init {
        includeAll.convention(false)
        skip.convention(false)
    }

    /**
     * Select an approximate percentage of tests using a hash of each test's identity.
     *
     * The selection is stable: it uses the fully qualified name and unique ID of each test.
     * Choose fast and stable tests, because the selected tests are required for merging to master even for unrelated changes.
     *
     * NOTE: it requires JUnit 5
     */
    fun includeAutoSamples(percentage: Int) {
        require(percentage in 0..100) { "percentage must be between 0 and 100, inclusive" }
        autoSamplePercentage.set(percentage)
    }

    fun includeAll() {
        includeAll.set(true)
    }

    fun skip() {
        skip.set(true)
    }
}

abstract class ContractTestsSelection {
    @get:Input
    abstract val skip: Property<Boolean>

    init {
        skip.convention(false)
    }

    fun skip() {
        skip.set(true)
    }
}
