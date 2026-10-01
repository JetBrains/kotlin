/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.support.ParameterDeclarations
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import java.util.stream.Stream
import kotlin.jvm.optionals.getOrNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParameterizedTestShardingTest {
    @Test
    fun `missing sharding call fails the invocations`() {
        val summary = execute(UnshardedParameterizedTest::class.java)
        assertEquals(2L, summary.testsFailedCount)
        assertTrue(summary.failures.all { it.exception.message.orEmpty().contains("requires a call") })
    }

    @Test
    fun `sharding in the arguments provider distributes the invocations across shards`() {
        val allTests = executeTests(TestShardingConfiguration(), ShardedParameterizedTest::class.java)
        assertEquals(listOf("test: a", "test: b", "test: c"), invocations(allTests))

        val shards = (1..3).map { shard ->
            invocations(executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3), ShardedParameterizedTest::class.java))
        }
        /* 3 invocations on 3 shards: one invocation per shard */
        shards.forEach { shard -> assertEquals(1, shard.size, "Expected one invocation per shard: $shards") }
        assertEquals(invocations(allTests), shards.flatten().sorted())
    }

    @Test
    fun `sharding works with composed annotations`() {
        val shards = (1..3).map { shard ->
            invocations(executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3), ComposedShardedParameterizedTest::class.java))
        }
        assertEquals(listOf("test: a", "test: b", "test: c"), shards.flatten().sorted())
    }

    @Test
    fun `sharding by method distributes all invocations`() {
        val shards = (1..3).map { shard ->
            invocations(executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3), ShardByMethodParameterizedTest::class.java))
        }
        assertEquals(listOf("test: a", "test: b", "test: c"), shards.flatten().sorted())
    }

    @Test
    fun `templates of a class with the same arguments run the same invocations`() {
        (1..3).forEach { shard ->
            val tests = executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3), MultipleShardedParameterizedTest::class.java)
            val first = invocations(tests).filter { it.startsWith("first: ") }.map { it.removePrefix("first: ") }
            val second = invocations(tests).filter { it.startsWith("second: ") }.map { it.removePrefix("second: ") }
            assertEquals(first, second, "Expected the same invocations of 'first' and 'second' on shard $shard")
        }
    }

    @Test
    fun `sharding without annotation keeps all invocations on one shard`() {
        val shards = (1..3).map { shard ->
            invocations(executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3), NotAnnotatedParameterizedTest::class.java))
        }
        assertEquals(1, shards.count { it.isNotEmpty() }, "Expected the template to run on exactly one shard: $shards")
        assertEquals(listOf("test: a", "test: b", "test: c"), shards.flatten().sorted())
    }

    @Test
    fun `sharding on a test method fails`() {
        val summary = execute(NotParameterizedTest::class.java)
        assertEquals(1L, summary.testsFailedCount)
        assertTrue(summary.failures.single().exception.message.orEmpty().contains("requires a call"))
    }

    @Test
    fun `invocations of multiple templates are distributed across shards`() {
        val allTests = executeTests(TestShardingConfiguration(), VersionsParameterizedTest::class.java)
        assertEquals(25, allTests.size)

        /* Shards without any invocation of the 'single' template do not fail (see 'executeTests') */
        val shards = (1..3).map { shard ->
            executeTests(TestShardingConfiguration(currentShard = shard, totalShards = 3), VersionsParameterizedTest::class.java)
        }
        assertEquals(invocations(allTests), shards.flatMap { invocations(it) }.sorted())

        /* Invocations of the same test template are distributed individually */
        val templatesOnMultipleShards = listOf("a", "b", "c", "single").filter { template ->
            shards.count { shard -> invocations(shard).any { it.startsWith("$template: ") } } > 1
        }
        assertTrue(templatesOnMultipleShards.isNotEmpty(), "Expected invocations of a test template to run on multiple shards")
    }

    class ShardingArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
            return listOf("a", "b", "c").map { Arguments.of(it) }.shard(context).toList().stream()
        }
    }

    @Tag(testFixtureTag)
    class UnshardedParameterizedTest {
        @ParameterizedTest
        @ParameterizedTestSharding
        @ValueSource(strings = ["a", "b"])
        fun test(@Suppress("unused") value: String) = Unit
    }

    @Tag(testFixtureTag)
    class ShardedParameterizedTest {
        @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
        @ParameterizedTestSharding
        @ArgumentsSource(ShardingArgumentsProvider::class)
        fun test(@Suppress("unused") value: String) = Unit
    }

    @Tag(testFixtureTag)
    @ShardByMethod
    class ShardByMethodParameterizedTest {
        @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
        @ParameterizedTestSharding
        @ArgumentsSource(ShardingArgumentsProvider::class)
        fun test(@Suppress("unused") value: String) = Unit
    }

    @Tag(testFixtureTag)
    class MultipleShardedParameterizedTest {
        @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
        @ParameterizedTestSharding
        @ArgumentsSource(ShardingArgumentsProvider::class)
        fun first(@Suppress("unused") value: String) = Unit

        @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
        @ParameterizedTestSharding
        @ArgumentsSource(ShardingArgumentsProvider::class)
        fun second(@Suppress("unused") value: String) = Unit
    }

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
    @ParameterizedTestSharding
    @ArgumentsSource(ShardingArgumentsProvider::class)
    annotation class ShardedTest

    @Tag(testFixtureTag)
    class ComposedShardedParameterizedTest {
        @ShardedTest
        fun test(@Suppress("unused") value: String) = Unit
    }

    @Tag(testFixtureTag)
    class NotAnnotatedParameterizedTest {
        @ParameterizedTest(name = "{0}")
        @ArgumentsSource(ShardingArgumentsProvider::class)
        fun test(@Suppress("unused") value: String) = Unit
    }

    @Tag(testFixtureTag)
    class NotParameterizedTest {
        @Test
        @ParameterizedTestSharding
        fun test() = Unit
    }

    class VersionsProvider : ArgumentsProvider {
        override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
            return (1..8).map { Arguments.of(it.toString()) }.shard(context).toList().stream()
        }
    }

    class SingleVersionProvider : ArgumentsProvider {
        override fun provideArguments(parameters: ParameterDeclarations, context: ExtensionContext): Stream<out Arguments> {
            return listOf(Arguments.of("1")).shard(context).toList().stream()
        }
    }

    @Tag(testFixtureTag)
    class VersionsParameterizedTest {
        @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
        @ParameterizedTestSharding
        @ArgumentsSource(VersionsProvider::class)
        fun a(@Suppress("unused") version: String) = Unit

        @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
        @ParameterizedTestSharding
        @ArgumentsSource(VersionsProvider::class)
        fun b(@Suppress("unused") version: String) = Unit

        @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
        @ParameterizedTestSharding
        @ArgumentsSource(VersionsProvider::class)
        fun c(@Suppress("unused") version: String) = Unit

        @ParameterizedTest(name = "{0}", allowZeroInvocations = true)
        @ParameterizedTestSharding
        @ArgumentsSource(SingleVersionProvider::class)
        fun single(@Suppress("unused") version: String) = Unit
    }

    /**
     * The executed invocations as '<template method>: <arguments>', sorted.
     * The unique ids of invocations are based on their index, which differs between shards, so they cannot be compared across shards.
     */
    private fun invocations(tests: List<TestIdentifier>): List<String> = tests.map { test ->
        val template = test.source.getOrNull() as? MethodSource ?: error("Missing method source of '$test'")
        "${template.methodName}: ${test.displayName}"
    }.sorted()

    /** Executes [testClass] without sharding, independently of the shard of this JVM (see 'tests.currentShard') */
    private fun execute(testClass: Class<*>): TestExecutionSummary {
        val listener = SummaryGeneratingListener()
        val launcher = LauncherFactory.create(LauncherConfig.builder().enablePostDiscoveryFilterAutoRegistration(false).build())
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(testClass))
            .configurationParameters(TestShardingConfiguration().toConfigurationParameters())
            .build()
        launcher.execute(request, listener)
        return listener.summary
    }
}
