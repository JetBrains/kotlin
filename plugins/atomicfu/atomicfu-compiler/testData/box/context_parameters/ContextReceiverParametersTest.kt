// LANGUAGE: +ContextParameters

import kotlinx.atomicfu.*
import kotlin.test.*

class ContextReceiverParametersFunctionTest() {
    private val a = atomic(0)
    private val arr = AtomicIntArray(10)

    context(c1: C1, c2: C2)
    private inline fun AtomicInt.extensionWithContextParameters(arg1: Int) {
        value = c1.n
        assertTrue(compareAndSet(c1.n, arg1))
        assertTrue(value == arg1)
        assertTrue(compareAndSet(arg1, c2.n))
        assertTrue(value == c2.n)
    }

    context(c1: C1, c2: C2)
    private inline fun AtomicInt.extensionWithContextParametersLoop(arg1: Int): Int {
        loop { cur ->
            assertTrue(compareAndSet(cur, c1.n))
            assertTrue(value == c1.n)
            assertTrue(compareAndSet(c1.n, c2.n))
            assertTrue(value == c2.n)
            return 777
        }
    }

    fun test() {
        val c1 = C1(111)
        val c2 = C2(222)
        with(c1) {
            with(c2) {
                a.extensionWithContextParameters(333)
                arr[2].extensionWithContextParameters(333)
                a.extensionWithContextParametersLoop(333)
                arr[3].extensionWithContextParametersLoop(333)
            }
        }
    }
}

private class C1(val n: Int)
private class C2 (val n: Int)

fun box(): String {
    val test = ContextReceiverParametersFunctionTest()
    test.test()
    return "OK"
}
