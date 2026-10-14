/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestShardingConfigurationTest {
    @Test
    fun `shard indices are zero based`() {
        assertTrue(TestShardingConfiguration(shardIndex = 0, shardCount = 1).isShardingEnabled)
        for (shardIndex in 0 until 3) {
            assertTrue(TestShardingConfiguration(shardIndex = shardIndex, shardCount = 3).isShardingEnabled)
        }
    }

    @Test
    fun `invalid shard indices and counts are rejected`() {
        for (shardIndex in listOf(-2, 3, Int.MAX_VALUE)) {
            assertFailsWith<IllegalStateException> {
                TestShardingConfiguration(shardIndex = shardIndex, shardCount = 3)
            }
        }
        for (shardCount in listOf(-2, 0)) {
            assertFailsWith<IllegalStateException> {
                TestShardingConfiguration(shardCount = shardCount)
            }
        }
    }

    @Test
    fun `minus one disables sharding`() {
        assertFalse(TestShardingConfiguration().isShardingEnabled)
        assertFalse(TestShardingConfiguration(shardIndex = -1, shardCount = 3).isShardingEnabled)
    }

    @Test
    fun `configuration parameters use build property names and zero based indices`() {
        assertEquals(
            mapOf(
                "kotlin.build.test.shard.index" to "0",
                "kotlin.build.test.shard.count" to "3",
                "kotlin.build.test.shard.seed" to "42",
                "tests.shardByMethod" to "false",
            ),
            TestShardingConfiguration(shardIndex = 0, shardCount = 3, shardSeed = 42).toConfigurationParameters()
        )
    }
}
