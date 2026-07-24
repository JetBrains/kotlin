// LANGUAGE: +CompanionBlocks +CompanionExtensions
// TARGET_BACKEND: JVM_IR

import kotlinx.atomicfu.*
import kotlin.test.*

private class DelegatedProperties {
    companion {
        val a = atomic(0)
        var dA: Int by a
        var d: Int by atomic(0)
    }
}

private companion val DelegatedProperties.tA = atomic(0)
private companion var DelegatedProperties.tdA: Int by DelegatedProperties.tA
private companion var DelegatedProperties.tD: Int by atomic(0)

private fun testDelegates() {
    assertEquals(0, DelegatedProperties.dA)
    DelegatedProperties.dA = 2
    assertEquals(2, DelegatedProperties.dA)
    assertEquals(2, DelegatedProperties.a.value)
    assertEquals(3, DelegatedProperties.a.incrementAndGet())
    assertEquals(3, DelegatedProperties.dA)

    assertEquals(0, DelegatedProperties.d)
    DelegatedProperties.d = 42
    assertEquals(42, DelegatedProperties.d)

    assertEquals(0, DelegatedProperties.tdA)
    DelegatedProperties.tdA = 2
    assertEquals(2, DelegatedProperties.tdA)
    assertEquals(2, DelegatedProperties.tA.value)
    assertEquals(3, DelegatedProperties.tA.incrementAndGet())
    assertEquals(3, DelegatedProperties.tdA)

    assertEquals(0, DelegatedProperties.tD)
    DelegatedProperties.tD = 42
    assertEquals(42, DelegatedProperties.tD)
}

fun box(): String {
    testDelegates()
    return "OK"
}

