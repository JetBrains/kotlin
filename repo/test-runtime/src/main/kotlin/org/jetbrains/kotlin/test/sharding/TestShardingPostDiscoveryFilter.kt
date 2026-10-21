/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor
import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.FilterResult.excluded
import org.junit.platform.engine.FilterResult.included
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestTag
import org.junit.platform.launcher.PostDiscoveryFilter
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.jvm.optionals.getOrNull

internal const val testsShardParameterizedTagKey = "tests.shard.parameterized"
internal val testsShardParameterizedTag = TestTag.create(testsShardParameterizedTagKey)

internal const val testsShardByMethodTagKey = "tests.shard.byMethod"
internal val testsShardByMethodTag = TestTag.create(testsShardByMethodTagKey)

/**
 * Spreads the test methods of this class across shards.
 * By default, all tests of a class run on the same shard.
 *
 * A `@TestFactory` still runs on a single shard.
 * A `@TestTemplate` also runs on a single shard, unless it uses [ParameterizedTestSharding].
 * Ignored for the classes listed in the timings, which run whole on their shard (see [TimedTestSharding]).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag(testsShardByMethodTagKey)
annotation class ShardByMethod

/**
 * Registered by the service loader (with the configuration of the system properties),
 * or explicitly by tests running several shards within one JVM.
 */
internal class TestShardingPostDiscoveryFilter(
    private val configuration: TestShardingConfiguration = readTestShardingConfigurationFromSystemProperties(),
) : PostDiscoveryFilter {

    override fun apply(test: TestDescriptor): FilterResult {
        if (!configuration.isShardingEnabled) return included("No shards configured")
        val isTestMethod = test.type == TestDescriptor.Type.TEST || test.mayRegisterTests()
        if (!isTestMethod) return included("Classes/Containers are always enabled")
        val timedShard = test.timedShard()
        if (timedShard == null && testsShardParameterizedTag in test.tags) return included("Test invocations are sharded by parameters")

        val shardIndex = configuration.shardIndex
        val thisTestShard = timedShard ?: calculateTestShard(
            test.shardingDistributionKey(configuration).encodeToByteArray(), configuration.shardCount, configuration.shardSeed
        )
        return if (thisTestShard == shardIndex) {
            included("Current shard index: '$shardIndex'. Test shard index: '$thisTestShard'")
        } else {
            excluded("Current shard index: '$shardIndex'. Test shard index: '$thisTestShard'")
        }
    }
}

/**
 * Chooses the unit that moves between shards: an individual test or a whole class.
 * Tests with the same key always go to the same shard for a given seed and shard count.
 */
internal fun TestDescriptor.shardingDistributionKey(configuration: TestShardingConfiguration): String {
    val uniqueId = this.uniqueId
    val classDescriptor = findClassDescriptor()

    /*
    Certain test engines may at least require some thought on how to put tests into shards.
    While the junit-jupiter's behavior of using the uniqueId directly is a reasonable default,
    it was deliberately chosen to fail on unexpected test engines, to ensure them being handled
    and thought about instead of silently behaving undesirably
     */
    return when (uniqueId.engineId.get()) {
        "junit-jupiter" -> {
            if (configuration.shardByMethod || testsShardByMethodTag in tags) {
                uniqueId.toString()
            } else {
                classDescriptor?.uniqueId?.toString() ?: error("Missing class test descriptor for '$displayName'")
            }
        }
        /*
        The compiler grouping test engine tries to group work done within a test class, we therefore select
        the test class (parent) as the uniqueId instead of the full testId (which may be method based)
         */
        "kotlin-compiler-grouping-engine" -> classDescriptor?.uniqueId?.toString() ?: uniqueId.removeLastSegment().toString()
        else -> error("Unexpected Test Engine ID: ${uniqueId.engineId}")
    }
}

/**
 * The innermost class containing this test, found by traversing the parents,
 * or `null` if the test is not contained within a class.
 */
private fun TestDescriptor.findClassDescriptor(): ClassBasedTestDescriptor? =
    generateSequence(this) { it.parent.getOrNull() }.filterIsInstance<ClassBasedTestDescriptor>().firstOrNull()

/**
 * The shard of the test's class in the timings (see [TimedTestSharding]),
 * or `null` if the timings do not list the class.
 */
private fun TestDescriptor.timedShard(): Int? {
    val testClass = findClassDescriptor()?.testClass ?: return null
    return timedTestSharding?.shardFor(testClass.name)
}

/** Execution-time counterpart of [TestDescriptor.timedShard] */
internal fun ExtensionContext.timedShard(): Int? = timedTestSharding?.shardFor(requiredTestClass.name)

/**
 * Execution-time counterpart of [TestDescriptor.shardingDistributionKey], used to salt the invocations of a test template (see [shard]):
 * the unique id of the test when sharding by method, otherwise the name of its test class.
 * Only junit-jupiter provides an [ExtensionContext], so no other test engines need to be handled.
 */
internal fun ExtensionContext.shardingDistributionKey(configuration: TestShardingConfiguration): String {
    if (configuration.shardByMethod || testsShardByMethodTagKey in tags) {
        return uniqueId
    }

    return requiredTestClass.name
}

// MessageDigest is mutable: reuse one instance per thread, without sharing its state between threads.
private val hashing: ThreadLocal<MessageDigest> = ThreadLocal.withInitial {
    MessageDigest.getInstance("SHA-1")
}

/**
 * Uses [rendezvous hashing](https://en.wikipedia.org/wiki/Rendezvous_hashing): give each shard a deterministic,
 * random-looking score for this test's key, then return the zero-based index of the shard with the highest score.
 *
 * With the same seed and key, each shard keeps its score regardless of the total number of shards.
 * Increasing shardCount only moves tests that a new shard wins. Decreasing it only moves tests whose shard was removed.
 * This relies on keeping the remaining shard IDs unchanged.
 *
 * Scores spread keys across shards, but do not guarantee equal test counts or running times.
 */
internal fun calculateTestShard(distributionKey: ByteArray, shardCount: Int, shardSeed: Int): Int {
    var selectedShard = 0
    var highestWeight = ULong.MIN_VALUE

    for (shard in 0 until shardCount) {
        val score = calculateShardScore(shard, distributionKey, shardSeed)

        // On equal scores, keep the lower shard ID, since shards are visited in ascending order.
        if (score > highestWeight) {
            selectedShard = shard
            highestWeight = score
        }
    }

    return selectedShard
}

internal fun calculateShardScore(shard: Int, key: ByteArray, seed: Int): ULong {
    val hash = hashing.get()
    // Each score hashes only this (seed, shard, key), independently of previously visited shards.
    hash.reset()

    // Encode the seed in four bytes, the least significant byte first. Changing it reshuffles assignments.
    hash.updateInt(seed)

    // Encode the shard ID in four bytes too, so the fields have fixed boundaries
    hash.updateInt(shard)
    hash.update(key)
    return ByteBuffer.wrap(hash.digest()).getLong().toULong()
}

/**
 * Calculates a hash which is seeded by the [seed] (`kotlin.build.test.shard.seed`)
 */
internal fun calculateHash(value: ByteArray, seed: Int): ULong {
    val hash = hashing.get()
    hash.reset()
    hash.updateInt(seed)
    hash.update(value)
    return ByteBuffer.wrap(hash.digest()).getLong().toULong()
}

private fun MessageDigest.updateInt(value: Int) {
    update(value.toByte())
    update(value.shr(8).toByte())
    update(value.shr(16).toByte())
    update(value.shr(24).toByte())
}
