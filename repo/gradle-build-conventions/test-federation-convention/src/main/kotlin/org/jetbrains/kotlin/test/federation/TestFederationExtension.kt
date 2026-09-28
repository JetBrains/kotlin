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

/**
 * A Gradle extension that may be applied to every `Test` task.
 *
 * Example:
 * ```kotlin
 * testTask {
 *     testFederation {
 *         smokeTests {
 *             // Sample 5% of all tests and add them to the SmokeTest subset
 *             includeAutoSamples(percentage = 5)
 *
 *             // Execute all tests when the SmokeTests subset is requested
 *             // This makes SmokeTests equivalent to AllTests
 *             includeAll()
 *
 *             // Opt out from the SmokeTests subset
 *             // If only this subset was requested, skip the task
 *             skip()
 *         }
 *
 *         contractTests {
 *             // Opt out from the ContractTestsFor<Domain> subsets
 *             // If only those subsets were requested, skip the task
 *             skip()
 *         }
 *     }
 * }
 * ```
 * NOTE: it implements [CommandLineArgumentProvider] just to be later added to `jvmArgumentProviders`
 * and to make Gradle process its `@Input` and `@Nested` annotations (instead of registering all the
 * inputs manually)
 */
abstract class TestFederationExtension @Inject constructor() : CommandLineArgumentProvider {
    @get:Nested
    abstract val smokeTests: SmokeTestsSelection

    @get:Nested
    abstract val contractTests: ContractTestsSelection

    /**
     * Customize the `SmokeTests` subset
     */
    fun smokeTests(configure: SmokeTestsSelection.() -> Unit) {
        smokeTests.configure()
    }

    /**
     * Customize the `ContractTestsFor<Domain>` subsets
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
     * Sample an approximate percentage of all tests and add them to the SmokeTest subset.
     *
     * The selection is stable: it uses hash of each test's identity (fully qualified name and unique ID).
     * Choose fast and stable tests because the selected tests are required for merging to master even for unrelated changes.
     */
    fun includeAutoSamples(percentage: Int) {
        require(percentage in 0..100) { "percentage must be between 0 and 100, inclusive" }
        autoSamplePercentage.set(percentage)
    }

    /**
     * Execute all tests when the `SmokeTests` subset is requested.
     *
     * This makes `SmokeTests` equivalent to `AllTests`.
     */
    fun includeAll() {
        includeAll.set(true)
    }

    /**
     * Opt out from the `SmokeTests` subset.
     *
     * If only this subset was requested, skip the task.
     */
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

    /**
     * Opt out from the `ContractTestsFor<Domain>` subsets.
     *
     * If only those subsets were requested, skip the task.
     */
    fun skip() {
        skip.set(true)
    }
}
