/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicTestShardingTest {
    @Test
    fun `aborted factory remains aborted`() {
        val summary = execute(AbortedFactory::class.java)
        assertEquals(1L, summary.containersAbortedCount)
        assertEquals(0L, summary.totalFailureCount)
        assertEquals(0L, summary.testsStartedCount)
    }

    @Test
    fun `failed factory preserves original failure`() {
        val summary = execute(FailedFactory::class.java)
        val failure = summary.failures.single().exception
        assertEquals("Factory failed", failure.message)
        assertTrue(failure.suppressed.isEmpty())
        assertEquals(0L, summary.testsStartedCount)
    }

    @Test
    fun `missing sharding call fails before dynamic tests execute`() {
        val summary = execute(UnshardedFactory::class.java)
        assertEquals(1L, summary.containersFailedCount)
        assertTrue(summary.failures.single().exception.message.orEmpty().contains("requires a call"))
        assertEquals(0L, summary.testsStartedCount)
    }

    @Test
    fun `sharding inside a dynamic test does not satisfy factory validation`() {
        val summary = execute(LateShardingFactory::class.java)
        assertEquals(1L, summary.containersFailedCount)
        assertTrue(summary.failures.single().exception.message.orEmpty().contains("requires a call"))
        assertEquals(0L, summary.testsStartedCount)
    }

    @Test
    fun `sharding before returning allows dynamic tests to execute`() {
        val summary = execute(ShardedFactory::class.java)
        assertEquals(0L, summary.totalFailureCount)
        assertEquals(1L, summary.testsSucceededCount)
    }

    @Test
    fun `sharding distributes the dynamic tests across shards`() {
        /* The unique ids of dynamic tests are based on their index, which differs between shards: compare their names instead */
        val allTests = executeTests(TestShardingConfiguration(), DistributedFactory::class.java).map { it.displayName }
        assertEquals((1..16).map { it.toString() }, allTests)

        val shard1 = executeTests(TestShardingConfiguration(currentShard = 1, totalShards = 2), DistributedFactory::class.java).map { it.displayName }
        val shard2 = executeTests(TestShardingConfiguration(currentShard = 2, totalShards = 2), DistributedFactory::class.java).map { it.displayName }
        assertTrue(shard1.isNotEmpty() && shard2.isNotEmpty(), "Expected both shards to contain tests: $shard1, $shard2")
        assertEquals(emptySet(), shard1.intersect(shard2.toSet()), "Expected no tests on both shards")
        assertEquals(allTests.sorted(), (shard1 + shard2).sorted())
    }

    @Tag(testFixtureTag)
    class AbortedFactory {
        @TestFactory
        @DynamicTestSharding
        context(_: DynamicTestShardingContext)
        fun tests(): List<DynamicTest> {
            assumeTrue(false)
            return emptyList()
        }
    }

    @Tag(testFixtureTag)
    class FailedFactory {
        @TestFactory
        @DynamicTestSharding
        context(_: DynamicTestShardingContext)
        fun tests(): List<DynamicTest> {
            error("Factory failed")
        }
    }

    @Tag(testFixtureTag)
    class UnshardedFactory {
        @TestFactory
        @DynamicTestSharding
        context(_: DynamicTestShardingContext)
        fun tests() = listOf(dynamicTest("unsharded") {})
    }

    @Tag(testFixtureTag)
    class LateShardingFactory {
        @TestFactory
        @DynamicTestSharding
        context(_: DynamicTestShardingContext)
        fun tests() = listOf(dynamicTest("late sharding") {
            emptyList<String>().shardBy { it }
        })
    }

    @Tag(testFixtureTag)
    class ShardedFactory {
        @TestFactory
        @DynamicTestSharding
        context(_: DynamicTestShardingContext)
        fun tests(): List<DynamicTest> {
            emptyList<String>().shardBy { it }
            return listOf(dynamicTest("sharded") {})
        }
    }

    @Tag(testFixtureTag)
    class DistributedFactory {
        @TestFactory
        @DynamicTestSharding
        context(_: DynamicTestShardingContext)
        fun tests() = (1..16).shardBy { it.toString() }.map { name ->
            dynamicTest(name.toString()) {}
        }
    }

    private fun execute(testClass: Class<*>): TestExecutionSummary {
        val listener = SummaryGeneratingListener()
        /* Without sharding, independently of the shard of this JVM (see 'tests.currentShard') */
        val launcher = LauncherFactory.create(LauncherConfig.builder().enablePostDiscoveryFilterAutoRegistration(false).build())
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(selectClass(testClass))
            .configurationParameters(TestShardingConfiguration().toConfigurationParameters())
            .build()
        launcher.execute(request, listener)
        return listener.summary
    }
}
