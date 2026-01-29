/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

import org.jetbrains.benchmarksLauncher.Random

class ChunkBuffer(var readPosition: Int, var writePosition: Int = readPosition + Random.nextInt(50)) {
    private val nextRef: AtomicRef<ChunkBuffer?> = atomic(null)

    /**
     * Reference to next buffer view. Useful to chain multiple views.
     * @see appendNext
     * @see cleanNext
     */
    var next: ChunkBuffer? get() = nextRef.value
        set(newValue) {
            if (newValue == null) {
                cleanNext()
            } else {
                appendNext(newValue)
            }
        }

    fun cleanNext(): ChunkBuffer? {
        return nextRef.getAndSet(null)
    }

    private fun appendNext(chunk: ChunkBuffer) {
        if (!nextRef.compareAndSet(null, chunk)) {
            throw IllegalStateException("This chunk has already a next chunk.")
        }
    }

    inline val readRemaining: Int get() = writePosition - readPosition
}

fun ChunkBuffer.remainingAll(): Long = remainingAll(0L)

private tailrec fun ChunkBuffer.remainingAll(n: Long): Long {
    val rem = readRemaining.toLong() + n
    val next = this.next ?: return rem
    return next.remainingAll(rem)
}

class LinkedListOfBuffers(var head: ChunkBuffer = ChunkBuffer(0,0),
                          var remaining: Long = head.remainingAll()) {
     var tailRemaining: Long = remaining - head.readRemaining
        set(newValue) {
            if (newValue < 0) {
                error("tailRemaining is negative: $newValue")
            }
            val tailSize = head.next?.remainingAll() ?: 0L
            if (newValue == 0L) {
                if (tailSize != 0L) {
                    error("tailRemaining is set 0 while there is a tail of size $tailSize")
                }
            }

            field = newValue
        }
}

open class LinkedListWithAtomicsBenchmark {
    val list: LinkedListOfBuffers
    init {
        val chunks: MutableList<ChunkBuffer> = ArrayList()
        (0..BENCHMARK_SIZE/2).forEachIndexed { index, i ->
            val chunk = ChunkBuffer(Random.nextInt())
            chunks.add(chunk)
            if (i > 0)
                chunks[i - 1].next = chunk
        }
        list = LinkedListOfBuffers(chunks[0])
    }

    tailrec fun ensureNext(current: ChunkBuffer = list.head): ChunkBuffer? {
        val next = current.next
        return when {
            next == null -> null
            else -> {
                list.tailRemaining = Random.nextInt().toLong() + 1
                ensureNext(next)
            }
        }
    }
}

