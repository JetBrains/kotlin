// LANGUAGE: +CompanionExtensions +CompanionBlocks
// TARGET_BACKEND: JVM_IR

import kotlinx.atomicfu.*
import kotlin.test.*

private inline val AtomicInt.twiceAsLarge: Int get() = value * 2

public class Outer {
    companion {
        private val a = atomic(10)

        fun test() {
            assertEquals(20, a.twiceAsLarge)
        }
    }
}

private companion val Outer.cA = atomic(1)

fun box(): String {
    Outer.test()
    assertEquals(2, Outer.cA.twiceAsLarge)
    return "OK"
}
