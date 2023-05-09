/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.membench

import kotlin.native.runtime.GC
import kotlin.native.runtime.GCInfo
import kotlin.native.ref.*
import kotlin.concurrent.*

fun fact(i: Long): Long = when (i) {
    0L -> 1
    else -> i * fact(i - 1)
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.ExperimentalStdlibApi::class)
class GCStat() { // TODO private constructor
    val recorded = mutableListOf<GCInfo>()
    var lastRecordedEpoch: Long = -1
    var pending = AtomicInt(0)
    @Volatile
    var shutdownRequested = false

    fun pauseTimes() = recorded.map { it.pauseEndTimeNs - it.pauseStartTimeNs }.toList()
    fun markedCount() = recorded.map { it.markedCount }.toList()
    fun pausePerObj() = recorded.map { (it.pauseEndTimeNs - it.pauseStartTimeNs).toDouble() / it.markedCount }.toList()

    data class Token(val owner: GCStat)

    init {
        scheduleNext()
    }

    fun scheduleNext() {
        pending.getAndIncrement()
        createCleaner(Token(this), { GCStat.tryRecord(it) })
    }

    fun shutdown() {
        shutdownRequested = true
        GC.collect()
        while (pending.value > 0) {

        }
    }

    companion object {
        inline fun <T> withStats(action: (GCStat) -> T): T {
            val gcstat = GCStat()
            val result = action(gcstat)
            gcstat.shutdown()
            return result
        }

        private fun tryRecord(token: Token): Boolean {
            val self = token.owner
            try {
                val gcInfo = GC.lastGCInfo
                gcInfo?.apply {
                    if (epoch > self.lastRecordedEpoch) {
                        self.recorded.add(this)
                        self.lastRecordedEpoch = epoch
                        if (!self.shutdownRequested) {
                            self.scheduleNext()
                        }
                        tryRecord@ return true
                    }
                }
                return false
            } finally {
                self.pending.getAndDecrement()
            }
        }
    }
}
