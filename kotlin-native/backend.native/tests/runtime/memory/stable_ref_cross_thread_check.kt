/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.memory.stable_ref_cross_thread_check

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlinx.cinterop.*

@Test
fun runTest1() {
    val worker = Worker.start()

    val future = worker.execute(TransferMode.SAFE, { }) {
        StableRef.create(Any())
    }
    val ref = future.result
    assertFailsWith<IncorrectDereferenceException> {
        val value = ref.get()
        println(value.toString())
    }

    worker.requestTermination().result
}

@Test
fun runTest2() {
    val worker = Worker.start()

    val mainThreadRef = StableRef.create(Any())
    // Simulate this going through interop as raw C pointer.
    val pointerValue: Long = mainThreadRef.asCPointer().toLong()
    val future = worker.execute(TransferMode.SAFE, { pointerValue }) {
        val pointer: COpaquePointer = it.toCPointer()!!
        assertFailsWith<IncorrectDereferenceException> {
            // Even attempting to convert a pointer to StableRef should fail.
            val otherThreadRef: StableRef<Any> = pointer.asStableRef()
            println(otherThreadRef.toString())
        }
        Unit
    }
    future.result

    worker.requestTermination().result
}
