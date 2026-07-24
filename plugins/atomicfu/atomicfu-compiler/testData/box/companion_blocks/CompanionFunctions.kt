// LANGUAGE: +CompanionBlocks +CompanionExtensions
// TARGET_BACKEND: JVM_IR

import kotlinx.atomicfu.*
import kotlin.test.*

private inline fun AtomicLong.inc(): Long = this.incrementAndGet()

public class C {
    companion {
        private val x = atomic(42L)

        fun test(): Long = x.inc()
    }
}

private companion val C.a = atomic(0L)
private companion fun C.aInc(): Long = a.incrementAndGet()
private companion val C.arr = AtomicLongArray(10)

fun box(): String {
    assertEquals(1L, C.a.inc())
    assertEquals(43L, C.test())
    assertEquals(2L, C.aInc())
    assertEquals(1L, C.arr[0].inc())
    return "OK"
}
