/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.nightly

import org.jetbrains.kotlin.test.sharding.TestShardingPostDiscoveryFilter
import org.jetbrains.kotlin.test.sharding.testFixtureTag
import org.jetbrains.kotlin.testFederation.NightlyTest
import org.jetbrains.kotlin.testFederation.TestFederationPostDiscoveryFilter
import org.jetbrains.kotlin.testFederation.nightlyTagKey
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.EngineFilter.includeEngines
import org.junit.platform.launcher.LauncherDiscoveryListener
import org.junit.platform.launcher.PostDiscoveryFilter
import org.junit.platform.launcher.TagFilter.excludeTags
import org.junit.platform.launcher.TagFilter.includeTags
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import java.util.ServiceLoader
import kotlin.jvm.optionals.getOrNull
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NightlyTestPostDiscoveryFilterTest {
    /// Test setup
    @NightlyTest
    annotation class FakeNightlyTest

    @Tag(testFixtureTag)
    @NightlyTest
    class NightlyClass {
        @Test
        fun a() = Unit

        @Test
        fun b() = Unit
    }

    @Tag(testFixtureTag)
    class NightlyMethods {
        @Test
        fun regular() = Unit

        @NightlyTest
        @Test
        fun nightly() = Unit

        @NightlyTest
        @TestFactory
        fun nightlyFactory() = listOf(dynamicTest("1") { })
    }

    @Tag(testFixtureTag)
    @FakeNightlyTest
    class ComposedNightlyClass {
        @Test
        fun a() = Unit
    }

    @Tag(testFixtureTag)
    class RegularClass {
        @Test
        fun a() = Unit
    }
    ///

    @Test
    fun `nightly tests are discovered when the nightly tag is included`() {
        assertEquals(
            setOf(
                "NightlyClass.a",
                "NightlyClass.b",
                "NightlyMethods.nightly",
                "NightlyMethods.nightlyFactory",
                "ComposedNightlyClass.a"
            ),
            discover(listOf(includeTags(nightlyTagKey)), allTestClasses, true),
        )
    }

    @Test
    fun `nightly tests are discovered when the nightly tag is required by a tag expression`() {
        assertEquals(
            setOf(
                "NightlyClass.a",
                "NightlyClass.b",
                "NightlyMethods.nightly",
                "NightlyMethods.nightlyFactory",
                "ComposedNightlyClass.a"
            ),
            discover(listOf(includeTags("$nightlyTagKey & $testFixtureTag")), allTestClasses, true),
        )
    }

    @Test
    fun `nightly tests are not discovered when the nightly tag is excluded`() {
        assertEquals(
            setOf("NightlyMethods.regular", "RegularClass.a"),
            discover(listOf(excludeTags(nightlyTagKey)), allTestClasses, true),
        )
    }

    @Test
    fun `regular tests are not checked on TeamCity`() {
        assertEquals(
            setOf("RegularClass.a"),
            discover(noFilters, listOf(RegularClass::class.java), true)
        )
    }

    @Test
    fun `nightly tests are not checked outside TeamCity`() {
        assertEquals(
            sortedSetOf(
                "NightlyClass.a",
                "NightlyClass.b",
                "NightlyMethods.regular",
                "NightlyMethods.nightly",
                "NightlyMethods.nightlyFactory",
                "ComposedNightlyClass.a",
                "RegularClass.a"
            ),
            discover(noFilters, allTestClasses, isTeamCity = false),
        )
    }

    @Test
    fun `discovery fails when nightly tests are selected regardless of the nightly tag`() {
        val requestFilters = mapOf(
            "no filters" to emptyList(),
            "another tag included" to listOf(includeTags(testFixtureTag)),
            "another tag excluded" to listOf(excludeTags("other")),
            "the nightly tag included as an alternative" to listOf(includeTags("$nightlyTagKey | $testFixtureTag")),
        )

        for ((description, filters) in requestFilters) {
            assertFailsWith<IllegalStateException>(
                "Expected the discovery with `$description` filter to fail when emulating teamcity"
            ) {
                discover(filters, allTestClasses, isTeamCity = true)
            }
        }
    }

    @Test
    fun `filter and listener are registered by the service loader`() {
        val filtersOfThisModule = ServiceLoader.load(PostDiscoveryFilter::class.java)
            .map { filter -> filter::class }
            .filter { filterClass -> filterClass.java.name.startsWith("org.jetbrains.kotlin.") }

        /* The check is registered first, so it is not skipped for the tests which the Test Federation or the sharding exclude */
        assertEquals(
            listOf(
                NightlyTestAssertPostDiscoveryFilter::class,
                TestFederationPostDiscoveryFilter::class,
                TestShardingPostDiscoveryFilter::class
            ),
            filtersOfThisModule,
        )
        assertTrue(ServiceLoader.load(LauncherDiscoveryListener::class.java).any { it is NightlyTestDiscoveryListener })
    }

    companion object {
        private val allTestClasses = listOf(
            NightlyClass::class.java,
            NightlyMethods::class.java,
            ComposedNightlyClass::class.java,
            RegularClass::class.java
        )

        private val noFilters = emptyList<PostDiscoveryFilter>()

        /**
         * Discovers the tests with the [requestFilters] (e.g., the tag filters of a Gradle test task),
         * and returns the discovered test methods as `Class.method`.
         * Only the nightly check is registered, instead of all post-discovery filters of this JVM (e.g., the Test Federation).
         */
        private fun discover(
            requestFilters: List<PostDiscoveryFilter>,
            testClasses: List<Class<*>>,
            isTeamCity: Boolean,
        ): Set<String> {
            val launcher = LauncherFactory.create(
                LauncherConfig.builder()
                    .enablePostDiscoveryFilterAutoRegistration(false)
                    .enableLauncherDiscoveryListenerAutoRegistration(false)
                    .addPostDiscoveryFilters(NightlyTestAssertPostDiscoveryFilter(isCheckEnabled = isTeamCity))
                    .addLauncherDiscoveryListeners(NightlyTestDiscoveryListener())
                    .build()
            )
            val request = LauncherDiscoveryRequestBuilder.request()
                .selectors(testClasses.map { selectClass(it) })
                .filters(includeEngines("junit-jupiter"), *requestFilters.toTypedArray())
                .build()

            val testPlan = launcher.discover(request)
            return testPlan.roots.flatMap { testPlan.getDescendants(it) }
                .mapNotNull { it.source.getOrNull() as? MethodSource }
                .map { source -> source.className.substringAfterLast("$") + "." + source.methodName }
                .toSortedSet()
        }
    }
}
