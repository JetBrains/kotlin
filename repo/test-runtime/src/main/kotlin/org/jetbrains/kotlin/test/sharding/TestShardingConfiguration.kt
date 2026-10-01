/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.jvm.optionals.getOrNull

/**
 * The sharding of a test run:
 * - [currentShard]: the 1-based shard to run (`tests.currentShard`), or `-1` to run all tests,
 * - [totalShards]: the number of shards (`tests.totalShards`), or `-1` when sharding is disabled,
 * - [shardSeed]: reshuffles the assignment of tests to shards (`tests.shardSeed`),
 * - [shardByMethod]: applies [ShardByMethod] to all test classes (`tests.shardByMethod`).
 *
 * This only holds the settings: the assignment of tests to shards is implemented by [calculateTestShard].
 * The Gradle test tasks pass these as system properties (see [readTestShardingConfigurationFromSystemProperties]).
 * Extensions read them from the JUnit configuration parameters, which fall back to the system properties
 * (see [readTestShardingConfiguration]), so that tests can run several shards within one JVM.
 */
internal data class TestShardingConfiguration(
    val currentShard: Int = -1,
    val totalShards: Int = -1,
    val shardSeed: Int = 0,
    val shardByMethod: Boolean = false,
) {
    /* Check inputs */
    init {
        if (currentShard != -1 && currentShard !in 1..totalShards) {
            error("Invalid 'currentShard': $currentShard; Expected 1..$totalShards")
        }

        if (totalShards != -1 && totalShards < 1) {
            error("Invalid 'totalShards': $totalShards; Expected >= 1")
        }
    }
}

private const val currentShardKey = "tests.currentShard"
private const val totalShardsKey = "tests.totalShards"
private const val shardSeedKey = "tests.shardSeed"
private const val shardByMethodKey = "tests.shardByMethod"

internal val TestShardingConfiguration.isShardingEnabled: Boolean
    get() = currentShard != -1 && totalShards != -1

internal fun readTestShardingConfigurationFromSystemProperties(): TestShardingConfiguration =
    readTestShardingConfiguration(System::getProperty)

/** Reads the JUnit configuration parameters of the test run, which fall back to the system properties. */
internal fun readTestShardingConfiguration(context: ExtensionContext): TestShardingConfiguration =
    readTestShardingConfiguration { key -> context.getConfigurationParameter(key).getOrNull() }

private fun readTestShardingConfiguration(value: (key: String) -> String?): TestShardingConfiguration = TestShardingConfiguration(
    currentShard = value(currentShardKey)?.toIntOrNull() ?: -1,
    totalShards = value(totalShardsKey)?.toIntOrNull() ?: -1,
    shardSeed = value(shardSeedKey)?.toIntOrNull() ?: 0,
    shardByMethod = value(shardByMethodKey) == "true",
)

/** All settings as JUnit configuration parameters (e.g., for a `LauncherDiscoveryRequestBuilder`), including the disabled ones. */
internal fun TestShardingConfiguration.toConfigurationParameters(): Map<String, String> = mapOf(
    currentShardKey to currentShard.toString(),
    totalShardsKey to totalShards.toString(),
    shardSeedKey to shardSeed.toString(),
    shardByMethodKey to shardByMethod.toString(),
)
