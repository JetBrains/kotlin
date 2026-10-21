/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.jetbrains.kotlin.test.sharding.ParameterizedTestShardingTest.ShardedParameterizedTest
import org.jetbrains.kotlin.test.sharding.TestShardingPostDiscoveryFilterTest.PlainTest1
import org.jetbrains.kotlin.test.sharding.TestShardingPostDiscoveryFilterTest.PlainTest2
import org.jetbrains.kotlin.test.sharding.TestShardingPostDiscoveryFilterTest.PlainTest3
import org.jetbrains.kotlin.test.sharding.TestShardingPostDiscoveryFilterTest.PlainTest4
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals

class TimedTestShardingTest {
    @Test
    fun `classes of all tasks go to the shard with the least time, longest first`() {
        val timings = listOf(
            ":a:test\tA1\t50",
            ":b:test\tB1\t40",
            ":a:test\tA2\t30",
            ":a:test\tA3\t20",
            ":b:test\tB2\t10",
        )
        val sharding = TimedTestSharding(timings, ":a:test", shardCount = 3)
        assertEquals(listOf(0, 2, 2, null, null), listOf("A1", "A2", "A3", "B1", "Unknown").map { sharding.shardFor(it) })
    }

    @Test
    fun `listed classes run whole on their shard, including parameterized invocations`(@TempDir directory: Path) {
        val timingsFile = directory.resolve("timings.tsv")
        timingsFile.writeText(
            listOf(ShardedParameterizedTest::class to 100, PlainTest1::class to 60, PlainTest2::class to 50, PlainTest3::class to 10)
                .joinToString("\n") { (testClass, time) -> ":p:test\t${testClass.java.name}\t$time" }
        )
        val testClasses = arrayOf(
            ShardedParameterizedTest::class.java, PlainTest1::class.java, PlainTest2::class.java, PlainTest3::class.java, PlainTest4::class.java
        )

        val allTests = executeTests(TestShardingConfiguration(), *testClasses)
        val shards = (0 until 3).map { shard ->
            executeTests(
                TestShardingConfiguration(shardIndex = shard, shardCount = 3, shardTimings = timingsFile.toString(), shardTask = ":p:test"),
                *testClasses,
            )
        }
        checkShardDistribution(allTests, *shards.toTypedArray())

        val listedClassesOfShards = shards.map { shard -> shard.map { it.className }.filter { it != PlainTest4::class.java.name }.toSet() }
        assertEquals(
            listOf(
                setOf(ShardedParameterizedTest::class.java.name),
                setOf(PlainTest1::class.java.name),
                setOf(PlainTest2::class.java.name, PlainTest3::class.java.name),
            ),
            listedClassesOfShards,
        )
        assertEquals(3, shards[0].count { it.className == ShardedParameterizedTest::class.java.name })
    }
}
