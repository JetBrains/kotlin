/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.process.CommandLineArgumentProvider
import javax.inject.Inject

/**
 * A Gradle extension that may be applied to every `Test` task.
 *
 * NOTE: it implements [CommandLineArgumentProvider] just to be later added to `jvmArgumentProviders`
 * and to make Gradle process its `@Input` and `@Nested` annotations (instead of registering all the
 * inputs manually)
 */
abstract class TestFederationExtension @Inject constructor() : CommandLineArgumentProvider {
    @get:Input
    abstract val declaredSubsets: SetProperty<TestSubset>

    @get:Nested
    abstract val smokeTests: SmokeTestsSelection

    @get:Input
    @get:Optional
    abstract val notCompatibleReason: Property<String>

    @get:Internal
    val allDeclaredSubsets: Provider<Set<TestSubset>>
        get() = declaredSubsets.map { subsets -> subsets + TestSubset.AllTests }

    /**
     * Declare the `SmokeTests` subset and optionally customize it
     */
    fun smokeTests(configure: SmokeTestsSelection.() -> Unit = {}) {
        declaredSubsets.add(TestSubset.SmokeTests)
        smokeTests.configure()
    }

    /**
     * Declare the `ContractTestsFor<Domain>` subsets of the given [domains]
     */
    fun contractTestsFor(vararg domains: Domain) {
        declaredSubsets.addAll(domains.map(::contractTestsSubsetOf))
    }

    /**
     * Opt out of partial test execution.
     *
     * When a partial subset (like `SmokeTests`) is requested, the test execution is skipped.
     *
     * When `AllTests` subset is requested, the tests are executed normally.
     */
    fun notCompatibleWithTestFederation(reason: String) {
        notCompatibleReason.set(reason)
    }

    override fun asArguments(): Iterable<String> = emptyList()
}

abstract class SmokeTestsSelection {
    @get:Input
    @get:Optional
    abstract val autoSamplePercentage: Property<Int>

    @get:Input
    abstract val includeAll: Property<Boolean>

    init {
        includeAll.convention(false)
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
     * This effectively makes `SmokeTests` equivalent to `AllTests`, making build cache entry reusable between those two runs.
     */
    fun includeAll() {
        includeAll.set(true)
    }
}
