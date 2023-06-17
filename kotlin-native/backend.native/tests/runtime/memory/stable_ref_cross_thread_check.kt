/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, FreezingIsDeprecated::class, ObsoleteWorkersApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package runtime.memory.stable_ref_cross_thread_check

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlinx.cinterop.*

class Holder(val value: Int)

@Test
fun runTest1() {
    val worker = Worker.start()

    val future = worker.execute(TransferMode.SAFE, { }) {
        StableRef.create(Holder(42))
    }
    val ref = future.result
    if (kotlin.native.Platform.memoryModel == kotlin.native.MemoryModel.EXPERIMENTAL) {
        val value = ref.get()
        assertEquals(value.value, 42)
    } else {
        assertFailsWith<IncorrectDereferenceException> {
            val value = ref.get()
            println(value.value)
        }
    }

    worker.requestTermination().result
}

@Test
fun runTest2() {
    val worker = Worker.start()

    val mainThreadRef = StableRef.create(Holder(42))
    // Simulate this going through interop as raw C pointer.
    val pointerValue: Long = mainThreadRef.asCPointer().toLong()
    val future = worker.execute(TransferMode.SAFE, { pointerValue }) {
        val pointer: COpaquePointer = it.toCPointer()!!
        if (kotlin.native.Platform.memoryModel == kotlin.native.MemoryModel.EXPERIMENTAL) {
            val otherThreadRef: StableRef<Holder> = pointer.asStableRef()
            assertEquals(otherThreadRef.get().value, 42)
        } else {
            assertFailsWith<IncorrectDereferenceException> {
                // Even attempting to convert a pointer to StableRef should fail.
                val otherThreadRef: StableRef<Holder> = pointer.asStableRef()
                println(otherThreadRef.get().value)
            }
        }
        Unit
    }
    future.result

    worker.requestTermination().result
}
