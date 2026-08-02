/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlin.native.concurrent.ObsoleteWorkersApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

package org.jetbrains.ring

import kotlin.native.concurrent.Worker
import kotlin.native.runtime.GC
import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val LONG_LIVED_COUNT = 131_072
private const val LONG_LIVED_PAYLOAD_SIZE = 16
private const val SHORT_LIVED_BATCHES = 64
private const val SHORT_LIVED_PER_BATCH = 4096
private const val GC_SCHEDULE_PERIOD = 4
private const val SMALL_OBJECT_GC_WAIT_SPINS = 100_000
private const val SMALL_OBJECT_FULL_GROWTH_TRIGGER_PERCENT = 1_000L

private class LongLivedNode(
    val id: Int,
    val payload: IntArray,
    var next: LongLivedNode? = null,
)

private class ShortLivedNode(
    val value: Int,
    val left: ShortLivedNode?,
    val right: ShortLivedNode?,
)

private fun newLongLivedGraph(): Array<LongLivedNode> {
    val nodes = Array(LONG_LIVED_COUNT) { index ->
        LongLivedNode(index, IntArray(LONG_LIVED_PAYLOAD_SIZE) { index + it })
    }
    for (index in nodes.indices) {
        nodes[index].next = nodes[(index + 1) % nodes.size]
    }
    return nodes
}

private fun churnShortLivedObjects(seed: Int): Int {
    var checksum = seed
    val young = arrayOfNulls<ShortLivedNode>(SHORT_LIVED_PER_BATCH)
    repeat(SHORT_LIVED_PER_BATCH) { index ->
        val left = if (index > 0) young[index - 1] else null
        val right = if (index > 1) young[index / 2] else null
        val current = ShortLivedNode(checksum + index, left, right)
        young[index] = current
        checksum += current.value
        if ((index and 31) == 0) {
            checksum += (current.left?.value ?: 0) - (current.right?.value ?: 0)
        }
    }
    for (index in young.indices step 257) {
        checksum = checksum xor young[index]!!.value
    }
    return checksum
}

private fun scheduleAndWaitSmallObjectGc(): Int {
    val collectionsBefore = GC.edenCollectionsCount + GC.fullCollectionsCount
    GC.schedule()
    var spins = 0
    while (GC.edenCollectionsCount + GC.fullCollectionsCount == collectionsBefore && spins < SMALL_OBJECT_GC_WAIT_SPINS) {
        Worker.current.park(0, process = false)
        spins += 1
    }
    return spins
}

@State(Scope.Benchmark)
@Measurement(time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
class GCSmallObjectChurnBenchmark : SkipWhenBaseOnly() {
    private val fullGrowthTriggerPercent = GC.fullGrowthTriggerPercent
    private val longLived = newLongLivedGraph()
    private var nextLongLived = 0

    init {
        GC.fullGrowthTriggerPercent = SMALL_OBJECT_FULL_GROWTH_TRIGGER_PERCENT
        GC.collect()
    }

    @Benchmark
    fun manyShortLivedSmallObjectsManyLongLivedObjects(bh: Blackhole) {
        skipWhenBaseOnly()

        var checksum = longLived[nextLongLived].id
        repeat(SHORT_LIVED_BATCHES) { batch ->
            val longLivedObject = longLived[nextLongLived]
            checksum += longLivedObject.payload[batch % LONG_LIVED_PAYLOAD_SIZE]
            checksum += longLivedObject.next!!.id
            nextLongLived = (nextLongLived + 1) % longLived.size

            checksum = churnShortLivedObjects(checksum)

            if ((batch + 1) % GC_SCHEDULE_PERIOD == 0) {
                checksum += scheduleAndWaitSmallObjectGc()
            }
        }

        bh.consume(checksum)
    }

    @TearDown
    fun restoreGcPolicy() {
        GC.fullGrowthTriggerPercent = fullGrowthTriggerPercent
    }
}
