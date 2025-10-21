/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.coverage.runtime

import kotlin.random.Random

// expect-actual
public object BooleanHitStorage {
    private val segments: MutableMap<Long, BooleanArray> = mutableMapOf()

    public fun reset() {
        synchronized(this) {
            segments.clear()
        }

    }

    public fun getOrCreateSegment(moduleId: Int, segmentNumber: Int, size: Int): BooleanArray {
        val id = (moduleId.toLong() shl 32) or segmentNumber.toLong()

        synchronized(this) {
            if (segments.isEmpty()) {
                initShutdownHook()
            }

            segments[id]?.let { return it }

            val array = BooleanArray(size)
            segments[id] = array
            return array
        }
    }

    public fun saveHits() {
        val dir = (System.getProperties()["kotlin.coverage.executions.path"] as? String) ?: "kover"
        val id = Random.nextLong()
        HitWriter.writeBoolean("$dir/coverage-$id.kex", segments)
    }

    private fun initShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            saveHits()
        })
    }
}


