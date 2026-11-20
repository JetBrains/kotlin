/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
    fun `equal times go in the order of the class names, each to the lowest of the shards with the least time`() {
        val timings = listOf(
            ":a:test\tA3\t10",
            "",
            ":a:test\tA1\t10",
            ":a:test\tA2\t10",
        )
        val sharding = TimedTestSharding(timings, ":a:test", shardCount = 2)
        assertEquals(listOf(0, 1, 0), listOf("A1", "A2", "A3").map { sharding.shardFor(it) })
    }

    @Test
    fun `invalid lines are rejected`() {
        for (line in listOf(":a:test\tA1", ":a:test\tA1\tslow", ":a:test\tA1\t10\t20")) {
            assertFailsWith<IllegalStateException> {
                TimedTestSharding(listOf(line), ":a:test", shardCount = 2)
            }
        }
    }

    @Test
    fun `a class is listed at most once per task`() {
        assertEquals(0, TimedTestSharding(listOf(":a:test\tA1\t10", ":b:test\tA1\t10"), ":a:test", shardCount = 2).shardFor("A1"))
        assertFailsWith<IllegalStateException> {
            TimedTestSharding(listOf(":a:test\tA1\t10", ":a:test\tA1\t20"), ":a:test", shardCount = 2)
        }
    }
}
