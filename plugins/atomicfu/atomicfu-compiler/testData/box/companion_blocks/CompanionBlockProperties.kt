// LANGUAGE: +CompanionBlocks
// TARGET_BACKEND: JVM_IR

import kotlinx.atomicfu.*
import kotlin.test.*

private class Outer {
    companion {
        internal val i = atomic(1)
        internal val b = atomic<Any?>(null)
        internal val arr = AtomicIntArray(2)
    }
}

fun testCompanionBlock() {
    assertEquals(1, Outer.i.value)
    assertEquals(2, Outer.i.incrementAndGet())

    assertNull(Outer.b.value)
    assertTrue(Outer.b.compareAndSet(null, "value"))
    assertEquals("value", Outer.b.value)

    assertEquals(0, Outer.arr[0].value)
    assertEquals(1, Outer.arr[0].incrementAndGet())
}

fun box(): String {
    testCompanionBlock()
    return "OK"
}
