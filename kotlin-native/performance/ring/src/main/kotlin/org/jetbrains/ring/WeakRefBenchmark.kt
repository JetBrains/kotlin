/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.ring

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.cinterop.StableRef
import org.jetbrains.benchmarksLauncher.Blackhole
import org.jetbrains.benchmarksLauncher.Random

private const val REPEAT_COUNT = BENCHMARK_SIZE
private const val REFERENCES_COUNT = 3

private class Data(var x: Int = Random.nextInt(1000) + 1)

private class ReferenceWrapper private constructor(
    data: Data
) {
    private val weak = WeakReference(data)
    private val strong = StableRef.create(data)

    val value: Int
        get() {
            val ref: Data? = weak.value
            if (ref == null) {
                return 0
            }
            return ref.x
        }

    fun dispose() {
        strong.dispose()
    }

    companion object {
        fun create() = ReferenceWrapper(Data())
    }
}

private fun ReferenceWrapper.stress() = (1..REPEAT_COUNT).sumOf {
    this.value
}

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
open class WeakRefBenchmark {
    private val aliveRef = ReferenceWrapper.create()
    private val deadRef = ReferenceWrapper.create().apply {
        dispose()
        GC.collect()
    }

    // Access alive reference.
    fun aliveReference() {
        assertNotEquals(0, aliveRef.stress())
    }

    // Access dead reference.
    fun deadReference() {
        assertEquals(0, deadRef.stress())
    }

    // Access reference that is nulled out in the middle.
    fun dyingReference() {
        val ref = ReferenceWrapper.create()

        ref.dispose()
        GC.schedule()

        Blackhole.consume(ref.stress())
    }
}
