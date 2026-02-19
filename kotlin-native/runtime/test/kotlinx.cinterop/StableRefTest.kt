/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.kotlinx.cinterop

import kotlinx.cinterop.*
import kotlin.native.concurrent.*
import kotlin.test.*

private class Box(val value: Int)

class StableRefTest {
    @Test
    fun crossThreadPassing() = withWorker {
        val future = execute(TransferMode.SAFE, {}) {
            StableRef.create(Box(42))
        }
        assertEquals(42, future.result.get().value)
    }

    @Test
    fun crossThreadPassingAsPointer() = withWorker {
        val mainThreadRef = StableRef.create(Box(42))
        // Simulate this going through interop as raw C pointer.
        val pointerValue: Long = mainThreadRef.asCPointer().toLong()
        val future = execute(TransferMode.SAFE, { pointerValue }) {
            val pointer: COpaquePointer = it.toCPointer()!!
            val otherThreadRef: StableRef<Box> = pointer.asStableRef()
            otherThreadRef.get().value
        }
        assertEquals(42, future.result)
    }
}