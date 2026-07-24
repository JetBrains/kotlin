// LANGUAGE: +CompanionExtensions +CompanionBlocks
// TARGET_BACKEND: JVM_IR

import kotlinx.atomicfu.*
import kotlin.test.*

public class Outer

private companion val Outer.cA = atomic(1)
private companion val Outer.cB = atomic<Any?>(null)
private companion val Outer.cArr = AtomicIntArray(2)

fun testCompanionExt() {
    assertEquals(1, Outer.cA.value)
    assertEquals(2, Outer.cA.incrementAndGet())

    assertNull(Outer.cB.value)
    assertTrue(Outer.cB.compareAndSet(null, "value"))
    assertEquals("value", Outer.cB.value)

    assertEquals(0, Outer.cArr[0].value)
    assertEquals(1, Outer.cArr[0].incrementAndGet())
}

fun box(): String {
    testCompanionExt()
    return "OK"
}
