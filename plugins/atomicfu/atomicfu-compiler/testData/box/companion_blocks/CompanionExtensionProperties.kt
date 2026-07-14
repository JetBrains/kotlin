// LANGUAGE: +CompanionExtensions +CompanionBlocks
// TARGET_BACKEND: JVM_IR

import kotlinx.atomicfu.*
import kotlin.test.*

public class Outer

private companion val Outer.cA = atomic(1)
private companion val Outer.cB = atomic<Any?>(null)

fun testCompanionExt() {
    assertEquals(1, Outer.cA.value)
    assertEquals(2, Outer.cA.incrementAndGet())
}

fun box(): String {
    testCompanionExt()
    return "OK"
}
