/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.nightly

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.test.sharding.TestShardingPostDiscoveryFilter
import org.jetbrains.kotlin.test.sharding.testFixtureTag
import org.jetbrains.kotlin.testFederation.NightlyTest
import org.jetbrains.kotlin.testFederation.TestFederationPostDiscoveryFilter
import org.jetbrains.kotlin.testFederation.nightlyTagKey
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.platform.engine.UniqueId
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
import java.util.*
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NightlyTestPostDiscoveryFilterTest {
    /// Test setup
    @NightlyTest
    annotation class IndirectNightlyTest

    @Tag(testFixtureTag)
    @NightlyTest
    class NightlyClass {
        @Test
        fun a_nightly() = Unit

        @Test
        fun b_nightly() = Unit
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
    @IndirectNightlyTest
    class ComposedNightlyClass {
        @Test
        fun a_nightly() = Unit
    }

    @Tag(testFixtureTag)
    class RegularClass {
        @Test
        fun normal() = Unit
    }

    @Tag(testFixtureTag)
    @Tag("other")
    class OtherTests {
        @NightlyTest
        @Test
        fun nightly() = Unit

        @Test
        fun normal() = Unit
    }

    @Tag(testFixtureTag)
    class NightlyParameterizedOnly {
        @NightlyTest
        @ParameterizedTest
        @ValueSource(ints = [1, 2])
        fun nightlyParameterized(@Suppress("unused") value: Int) = Unit
    }
    ///

    @Test
    fun `only nightly tests are discovered when the non-nightly tag is excluded - no filter activation`() {
        assertEquals(
            sortedSetOf(
                "NightlyClass.a_nightly",
                "NightlyClass.b_nightly",
                "NightlyMethods.nightly",
                "NightlyMethods.nightlyFactory",
                "NightlyParameterizedOnly.nightlyParameterized",
                "ComposedNightlyClass.a_nightly",
                "OtherTests.nightly",
            ),
            discover(listOf(excludeTags("!$nightlyTagKey")), allTestClasses, true),
        )
    }

    @Test
    fun `nightly tests are discovered when the nightly tag is required by a tag expression - no filter activation`() {
        assertEquals(
            sortedSetOf(
                "NightlyClass.a_nightly",
                "NightlyClass.b_nightly",
                "NightlyMethods.nightly",
                "NightlyMethods.nightlyFactory",
                "NightlyParameterizedOnly.nightlyParameterized",
                "ComposedNightlyClass.a_nightly",
                "OtherTests.nightly"
            ),
            discover(listOf(includeTags("$nightlyTagKey & $testFixtureTag")), allTestClasses, true),
        )
    }

    @Test
    fun `nightly tests are not discovered when the nightly tag is excluded - no filter activation`() {
        assertEquals(
            sortedSetOf("NightlyMethods.regular", "RegularClass.normal", "OtherTests.normal"),
            discover(listOf(excludeTags(nightlyTagKey)), allTestClasses, true),
        )
    }

    @Test
    fun `regular tests are not checked on TeamCity - no filter activation`() {
        assertEquals(
            sortedSetOf("RegularClass.normal"),
            discover(noFilters, listOf(RegularClass::class.java), true)
        )
    }

    @Test
    fun `nightly tests are not checked outside TeamCity - allow general purpose local tasks`() {
        assertEquals(
            sortedSetOf(
                "ComposedNightlyClass.a_nightly",
                "NightlyClass.a_nightly",
                "NightlyClass.b_nightly",
                "NightlyMethods.regular",
                "NightlyMethods.nightly",
                "NightlyMethods.nightlyFactory",
                "NightlyParameterizedOnly.nightlyParameterized",
                "OtherTests.nightly",
                "OtherTests.normal",
                "RegularClass.normal"
            ),
            discover(noFilters, allTestClasses, isTeamCity = false),
        )
    }

    @Test
    fun `discovery fails when nightly tests are selected regardless of the nightly tag`() {
        val requestFilters = mapOf(
            "no filters" to emptyList(),
            "another tag included" to listOf(includeTags(testFixtureTag)),
            "another tag excluded" to listOf(excludeTags("some")),
            "the nightly tag included as an alternative" to listOf(includeTags("$nightlyTagKey | $testFixtureTag")),
            "the nightly tag included as implicit OR" to listOf(includeTags(nightlyTagKey, "other")),
        )

        for ((description, filters) in requestFilters) {
            assertFailsWith<InsufficientNightlyFilteringException>(
                "Expected the discovery with `$description` filter to fail when emulating teamcity"
            ) {
                discover(filters, allTestClasses, isTeamCity = true)
            }
        }
    }

    @Test
    fun `parallel discoveries are checked against their own requests`() {
        // The test reacts if there's no thread local used in the NightlyTestAssertPostDiscoveryFilter.kt
        runBlocking {
            /* Both discoveries register their requests before any of them applies the post-discovery filters */
            val barrier = CyclicBarrier(2)
            val awaitOtherDiscovery = object : LauncherDiscoveryListener {
                override fun engineDiscoveryStarted(engineId: UniqueId) {
                    barrier.await(10, TimeUnit.SECONDS)
                }
            }

            launch(Dispatchers.IO) {
                assertEquals(
                    sortedSetOf("NightlyClass.a_nightly", "NightlyClass.b_nightly"),
                    discover(
                        listOf(excludeTags("!$nightlyTagKey")),
                        listOf(NightlyClass::class.java),
                        true,
                        listOf(awaitOtherDiscovery),
                    ),
                )
            }
            launch(Dispatchers.IO) {
                assertFailsWith<InsufficientNightlyFilteringException> {
                    discover(noFilters, listOf(NightlyClass::class.java), true, listOf(awaitOtherDiscovery))
                }
            }
        }
    }

    @Test
    fun `nested discovery does not affect the request of the outer discovery`() {
        /* Emulates the JUnit Platform Suite Engine, which discovers the suite tests during the engine discovery */
        var nestedTests: Set<String>? = null
        val nestedDiscovery = object : LauncherDiscoveryListener {
            override fun engineDiscoveryStarted(engineId: UniqueId) {
                if (nestedTests == null) {
                    nestedTests = discover(noFilters, listOf(RegularClass::class.java), isTeamCity = true)
                }
            }
        }

        assertEquals(
            sortedSetOf("NightlyClass.a_nightly", "NightlyClass.b_nightly"),
            discover(listOf(excludeTags("!$nightlyTagKey")), listOf(NightlyClass::class.java), true, listOf(nestedDiscovery)),
        )
        assertEquals(sortedSetOf("RegularClass.normal"), nestedTests)
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
            RegularClass::class.java,
            OtherTests::class.java,
            NightlyParameterizedOnly::class.java
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
            additionalListeners: List<LauncherDiscoveryListener> = emptyList(),
        ): Set<String> {
            val launcher = LauncherFactory.create(
                LauncherConfig.builder()
                    .enablePostDiscoveryFilterAutoRegistration(false)
                    .enableLauncherDiscoveryListenerAutoRegistration(false)
                    .addPostDiscoveryFilters(NightlyTestAssertPostDiscoveryFilter(isCheckEnabled = isTeamCity))
                    .addLauncherDiscoveryListeners(NightlyTestDiscoveryListener(), *additionalListeners.toTypedArray())
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
