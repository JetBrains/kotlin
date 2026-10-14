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
 * - [shardByMethod]: applies [ShardByMethod] to all test classes (`tests.shardByMethod`).
 *
 * This only holds the settings: the assignment of tests to shards is implemented by [calculateTestShard].
 * The Gradle test tasks pass these as system properties (see [readTestShardingConfigurationFromSystemProperties]).
 * Extensions read them from the JUnit configuration parameters, which fall back to the system properties
 * (see [readTestShardingConfiguration]), so that tests can run several shards within one JVM.
 */
internal data class TestShardingConfiguration(
    val shardIndex: Int = -1,
    val shardCount: Int = -1,
    val shardSeed: Int = 0,
    val shardByMethod: Boolean = false,
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

private const val shardIndexKey = "kotlin.build.test.shard.index"
private const val shardCountKey = "kotlin.build.test.shard.count"
private const val shardSeedKey = "kotlin.build.test.shard.seed"
private const val shardByMethodKey = "tests.shardByMethod"

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
)

/** All settings as JUnit configuration parameters (e.g., for a `LauncherDiscoveryRequestBuilder`), including the disabled ones. */
internal fun TestShardingConfiguration.toConfigurationParameters(): Map<String, String> = mapOf(
    shardIndexKey to shardIndex.toString(),
    shardCountKey to shardCount.toString(),
    shardSeedKey to shardSeed.toString(),
    shardByMethodKey to shardByMethod.toString(),
)
