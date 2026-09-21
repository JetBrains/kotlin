/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import javax.inject.Inject

abstract class TestFederationTaskExtension @Inject constructor() : CommandLineArgumentProvider {
    @get:Input
    abstract val runAllTestsAlways: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val runAllTestsOrSkip: Property<Boolean>

    @get:Nested
    abstract val smokeTests: SmokeTestsSelection

    @get:Nested
    abstract val contractTests: ContractTestsSelection

    init {
        runAllTestsAlways.convention(false)
        runAllTestsOrSkip.convention(false)
    }

    /**
     * If the `AllTests` subset was requested, run it normally.
     *
     * If any other test subset was requested, force `AllTests` anyway.
     *
     * Example:
     * ```kotlin
     * testFederation {
     *     runAllTestsAlways()
     * }
     */
    fun runAllTestsAlways() {
        check(!runAllTestsOrSkip.isPresent) { "Cannot combine 'runAllTestsAlways' with 'runAllTestsOrSkip'" }
        runAllTestsAlways.set(true)
    }

    /**
     * If the `AllTests` subset was requested, run it normally.
     *
     * If any other test subset was requested, skip the task.
     *
     * Example:
     * ```kotlin
     * testFederation {
     *     runAllTestsOrSkip()
     * }
     * ```
     */
    fun runAllTestsOrSkip() {
        check(!runAllTestsAlways.get()) { "Cannot combine 'runAllTestsOrSkip' with 'runAllTestsAlways'" }
        runAllTestsOrSkip.set(true)
    }

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
     *         // Add tests by pattern to the SmokeTest subset
     *         // (useful for JUnit3)
     *         includeTestsMatching("*SmokeTest")
     *     }
     * }
     * ```
     */
    fun smokeTests(configure: SmokeTestsSelection.() -> Unit) {
        smokeTests.configure()
    }

    /**
     * Configure additional filters for the `ContractTestsFor<Domain>` subsets.
     *
     * Useful for JUnit3 tests that don't support `@MustRunOnChangesIn` annotations.
     *
     * Example:
     * ```kotlin
     * testFederation {
     *     contractTests {
     *         forDomain(Wasm) {
     *             // Add tests by pattern to the ContractTestsForWasm subset
     *             // (useful for JUnit3)
     *             includeTestsMatching("*WasmTest")
     *         }
     *     }
     * }
     * ```
     */
    fun contractTests(configure: ContractTestsSelection.() -> Unit) {
        contractTests.configure()
    }

    override fun asArguments(): Iterable<String> = emptyList()
}

abstract class SmokeTestsSelection @Inject constructor() {
    @get:Input
    abstract val testNamePatterns: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val autoSamplePercentage: Property<Int>

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

    /**
     * Select tests by pattern. This is backed by a vanilla Gradle test filter.
     *
     * NOTE: only useful for JUnit 3
     */
    fun includeTestsMatching(pattern: String) {
        testNamePatterns.add(pattern)
    }
}

abstract class ContractTestsSelection @Inject constructor(objects: ObjectFactory) {
    @get:Nested
    val domains: NamedDomainObjectContainer<DomainTestsSelection> =
        objects.domainObjectContainer(DomainTestsSelection::class.java)

    fun forDomain(domain: Domain, configure: DomainTestsSelection.() -> Unit) {
        domains.maybeCreate(domain.name).configure()
    }
}

abstract class DomainTestsSelection @Inject constructor(private val domainName: String) : Named {
    override fun getName(): String = domainName

    @get:Input
    abstract val testNamePatterns: ListProperty<String>

    /**
     * Select tests by pattern. This is backed by a vanilla Gradle test filter.
     *
     * NOTE: only useful for JUnit 3
     */
    fun includeTestsMatching(pattern: String) {
        testNamePatterns.add(pattern)
    }
}
