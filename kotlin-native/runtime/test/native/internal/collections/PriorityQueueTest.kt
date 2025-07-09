/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.native.internal.collections

import kotlin.native.internal.collections.PriorityQueue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PriorityQueueTest {
    val testData = listOf(108, 42, 37, 4, 8, 15, 16, 23, 42)

    @Test
    fun addAndFirstElement() {
        val queue = PriorityQueue.minimal<Int>()
        queue.add(testData.first())
        assertFalse(queue.isEmpty())
        assertEquals(1, queue.size)
        assertEquals(testData.first(), queue.first())
    }

    @Test
    fun minHeap() {
        val queue = PriorityQueue.minimal<Int>()
        testData.forEach { queue.add(it) }

        assertEquals(
                testData.sorted(),
                generateSequence { queue.removeFirstOrNull() }.toList()
        )
    }

    @Test
    fun maxHeap() {
        val queue = PriorityQueue.maximal<Int>()
        testData.forEach { queue.add(it) }

        assertEquals(
                testData.sortedDescending(),
                generateSequence { queue.removeFirstOrNull() }.toList()
        )
    }
}