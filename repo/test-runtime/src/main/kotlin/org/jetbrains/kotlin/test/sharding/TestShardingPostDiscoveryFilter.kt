/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor
import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.FilterResult.excluded
import org.junit.platform.engine.FilterResult.included
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.launcher.PostDiscoveryFilter
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.Checksum
import kotlin.jvm.optionals.getOrNull

class TestShardingPostDiscoveryFilter : PostDiscoveryFilter {

    private val currentShard = System.getProperty("tests.currentShard")?.toIntOrNull() ?: -1
    private val totalShards = System.getProperty("tests.totalShards")?.toIntOrNull() ?: -1
    private val shardSeed = System.getProperty("tests.shardSeed")?.toIntOrNull() ?: 0

    /* Check inputs */
    init {
        if (currentShard != -1 && currentShard !in 1..totalShards) {
            error("Invalid 'currentShard': $currentShard; Expected 1..$totalShards")
        }

        if (totalShards != -1 && totalShards < 1) {
            error("Invalid 'totalShards': $totalShards; Expected >= 1")
        }
    }

    // MessageDigest is mutable: reuse one instance per thread, without sharing its state between threads.
    private val hashing: ThreadLocal<MessageDigest> = ThreadLocal.withInitial {
        MessageDigest.getInstance("SHA-1")
    }

    override fun apply(test: TestDescriptor): FilterResult {
        if (currentShard < 0 || totalShards < 0) return included("No shards configured")
        val isTestMethod = test.type == TestDescriptor.Type.TEST || test.mayRegisterTests()
        if (!isTestMethod) return included("Classes/Containers are always enabled")
        val thisTestShard = calculateTestShard(test)

        return if (thisTestShard == currentShard) {
            included("Current shard: '$currentShard'. Test shard: '$thisTestShard'")
        } else {
            excluded("Current shard: '$currentShard'. Test shard: '$thisTestShard'")
        }
    }

    /**
     * Uses [rendezvous hashing](https://en.wikipedia.org/wiki/Rendezvous_hashing): give each shard a deterministic,
     * random-looking score for this test's key, then choose the shard with the highest score.
     *
     * With the same seed and key, each shard keeps its score regardless of the total number of shards.
     * Increasing totalShards only moves tests that a new shard wins. Decreasing it only moves tests whose shard was removed.
     * This relies on keeping the remaining shard IDs unchanged.
     *
     * Scores spread keys across shards, but do not guarantee equal test counts or running times.
     */
    private fun calculateTestShard(test: TestDescriptor): Int {
        var selectedShard = 1
        var highestWeight = ULong.MIN_VALUE
        val hash = hashing.get()

        val distributionKey = distributionKey(test).encodeToByteArray()

        for (shard in 1..totalShards) {
            // Each score hashes only this (seed, shard, key), independently of previously visited shards.
            hash.reset()

            // Encode the seed in four bytes, the least significant byte first. Changing it reshuffles assignments.
            hash.update(shardSeed.toByte())
            hash.update(shardSeed.shr(8).toByte())
            hash.update(shardSeed.shr(16).toByte())
            hash.update(shardSeed.shr(24).toByte())

            // Encode the shard ID in four bytes too, so the fields have fixed boundaries
            hash.update(shard.toByte())
            hash.update(shard.shr(8).toByte())
            hash.update(shard.shr(16).toByte())
            hash.update(shard.shr(24).toByte())

            hash.update(distributionKey)
            // Use the first eight digest bytes as an unsigned score
            val weight = ByteBuffer.wrap(hash.digest()).getLong().toULong()

            // On equal scores, keep the lower shard ID, since shards are visited in ascending order.
            if (weight > highestWeight) {
                selectedShard = shard
                highestWeight = weight
            }
        }

        return selectedShard
    }


    /**
     * Checks the class and its superclasses for a method with the given annotation.
     * ClassValue caches the answer per class, so tests in the same class do not repeat the reflection work.
     */
    class HasMethodWithAnnotationClassValue(val annotationClass: Class<out Annotation>) : ClassValue<Boolean>() {
        override fun computeValue(type: Class<*>): Boolean? {
            if (type.declaredMethods.any { method ->
                    method.isAnnotationPresent(annotationClass)
                }) return true

            type.superclass?.let { superclass ->
                return this[superclass]
            }

            return false
        }
    }

    /**
     * Can be used to query if a class contains any [BeforeAll] annotation
     */
    val hasBeforeAll = HasMethodWithAnnotationClassValue(BeforeAll::class.java)

    /**
     * Can be used to query if a class contains any [AfterAll] annotation
     */
    val hasAfterAll = HasMethodWithAnnotationClassValue(AfterAll::class.java)

    /**
     * Chooses the unit that moves between shards: an individual test or a whole class.
     * Tests with the same key always go to the same shard for a given seed and shard count.
     */
    private fun distributionKey(test: TestDescriptor): String {
        val uniqueId = test.uniqueId

        /*
        If the test is contained within a class, then we can find the class by traversing the parents
         */
        val classDescriptor = generateSequence(test) { it.parent.getOrNull() }
            .filterIsInstance<ClassBasedTestDescriptor>()
            .firstOrNull()

        /*
        Certain test engines may at least require some thought on how to put tests into shards.
        While the junit-jupiter's behavior of using the uniqueId directly is a reasonable default,
        it was deliberately chosen to fail on unexpected test engines, to ensure them being handled
        and thought about instead of silently behaving undesirably
         */
        return when (uniqueId.engineId.get()) {
            "junit-jupiter" -> {
                /*
                If the class has @BeforeAll or @AfterAll methods (including inherited ones), use its class ID as the key.
                This keeps the class's tests together instead of repeating potentially expensive setup and teardown across shards.
                 */
                if (classDescriptor != null && (hasBeforeAll[classDescriptor.testClass] || hasAfterAll[classDescriptor.testClass])) {
                    classDescriptor.uniqueId.toString()
                }
                /*
                Otherwise, use the individual test's ID. More independent keys give the hash more chances to balance the shards.
                 */
                else uniqueId.toString()
            }

            /*
            The compiler grouping test engine tries to group work done within a test class, we therefore select
            the test class (parent) as the uniqueId instead of the full testId (which may be method based)
             */
            "kotlin-compiler-grouping-engine" -> classDescriptor?.uniqueId?.toString() ?: uniqueId.removeLastSegment().toString()
            else -> error("Unexpected Test Engine ID: ${uniqueId.engineId}")
        }
    }
}
