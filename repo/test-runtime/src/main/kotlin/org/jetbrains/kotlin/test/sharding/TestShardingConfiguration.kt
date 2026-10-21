/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.jvm.optionals.getOrNull

/**
 * The sharding of a test run:
 * - [shardIndex]: the zero-based shard index to run (`kotlin.build.test.shard.index`), or `-1` to run all tests,
 * - [shardCount]: the number of shards (`kotlin.build.test.shard.count`), or `-1` when sharding is disabled,
 * - [shardSeed]: reshuffles the assignment of tests to shards (`kotlin.build.test.shard.seed`),
 * - [shardByMethod]: applies [ShardByMethod] to all test classes (`tests.shardByMethod`),
 * - [shardTimings]: the path of the recorded times of test classes to balance the shards by (`kotlin.build.test.shard.timings`),
 * - [shardTask]: the path of the test task whose classes to take from [shardTimings] (`kotlin.build.test.shard.task`).
 *
 * This only holds the settings: the assignment of tests to shards is implemented by [calculateTestShard] and [TimedTestSharding].
 * The Gradle test tasks pass these as system properties (see [readTestShardingConfigurationFromSystemProperties]).
 * Extensions read them from the JUnit configuration parameters, which fall back to the system properties
 * (see [readTestShardingConfiguration]), so that tests can run several shards within one JVM.
 * Except for [shardTimings] and [shardTask]: they belong to the test task, so only those of the system properties are used,
 * once per JVM (see [timedTestSharding]).
 */
internal data class TestShardingConfiguration(
    val shardIndex: Int = -1,
    val shardCount: Int = -1,
    val shardSeed: Int = 0,
    val shardByMethod: Boolean = false,
    val shardTimings: String? = null,
    val shardTask: String? = null,
) {
    /* Check inputs */
    init {
        if (shardIndex != -1 && shardIndex !in 0 until shardCount) {
            error("Invalid 'shardIndex': $shardIndex; Expected 0 until $shardCount")
        }

        if (shardCount != -1 && shardCount < 1) {
            error("Invalid 'shardCount': $shardCount; Expected >= 1")
        }
    }
}

internal const val shardIndexKey = "kotlin.build.test.shard.index"
internal const val shardCountKey = "kotlin.build.test.shard.count"
internal const val shardSeedKey = "kotlin.build.test.shard.seed"
internal const val shardByMethodKey = "tests.shardByMethod"
private const val shardTimingsKey = "kotlin.build.test.shard.timings"
private const val shardTaskKey = "kotlin.build.test.shard.task"

internal val TestShardingConfiguration.isShardingEnabled: Boolean
    get() = shardIndex != -1 && shardCount != -1

internal fun readTestShardingConfigurationFromSystemProperties(): TestShardingConfiguration =
    readTestShardingConfiguration(System::getProperty)

/** Reads the JUnit configuration parameters of the test run, which fall back to the system properties. */
internal fun readTestShardingConfiguration(context: ExtensionContext): TestShardingConfiguration =
    readTestShardingConfiguration { key -> context.getConfigurationParameter(key).getOrNull() }

private fun readTestShardingConfiguration(value: (key: String) -> String?): TestShardingConfiguration = TestShardingConfiguration(
    shardIndex = value(shardIndexKey)?.toIntOrNull() ?: -1,
    shardCount = value(shardCountKey)?.toIntOrNull() ?: -1,
    shardSeed = value(shardSeedKey)?.toIntOrNull() ?: 0,
    shardByMethod = value(shardByMethodKey) == "true",
    shardTimings = value(shardTimingsKey),
    shardTask = value(shardTaskKey),
)
