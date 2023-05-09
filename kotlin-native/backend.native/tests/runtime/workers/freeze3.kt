/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(FreezingIsDeprecated::class, ObsoleteWorkersApi::class)

package runtime.workers.freeze3

import kotlin.test.*

import kotlin.native.concurrent.*

object AnObject {
    var x = 1
}

@ThreadLocal
object Mutable {
    var x = 2
}

val topLevelInline: ULong = 0xc3a5c85c97cb3127U

@Test fun runTest1() {
    assertEquals(1, AnObject.x)
    if (Platform.memoryModel == MemoryModel.STRICT) {
        assertFailsWith<InvalidMutabilityException> {
            AnObject.x++
        }
        assertEquals(1, AnObject.x)
    } else {
        AnObject.x++
        assertEquals(2, AnObject.x)
    }

    Mutable.x++
    assertEquals(3, Mutable.x)
    println("OK")
}

@Test fun runTest2() {
    val ok = AtomicInt(0)
    withWorker() {
     executeAfter(0, {
      assertEquals(0xc3a5c85c97cb3127U, topLevelInline) 
      ok.increment()
     }.freeze())
   }
   assertEquals(1, ok.value)
}

