import kotlinx.atomicfu.*
import kotlin.test.*

class LoopTest {
    private val a = atomic(0)
    private val r = atomic<A>(A("aaaa"))
    private val rs = atomic<String>("bbbb")

    private class A(val s: String)

    private inline fun casLoop(to: Int): Int {
        a.loop { cur ->
            if (a.compareAndSet(cur, to)) return a.value
            return 777
        }
    }

    private inline fun casLoopExpression(to: Int): Int = a.loop { cur ->
        if (a.compareAndSet(cur, to)) return a.value
        return 777
    }

    private inline fun AtomicInt.extensionLoop(to: Int): Int {
        loop { cur ->
            if (compareAndSet(cur, to)) return value
            return 777
        }
    }

    private inline fun AtomicInt.extensionLoopExpression(to: Int): Int = loop { cur ->
        lazySet(cur + 10)
        return if (compareAndSet(cur, to)) value else incrementAndGet()
    }

    private inline fun AtomicInt.extensionLoopMixedReceivers(first: Int, second: Int): Int {
        loop { cur ->
            compareAndSet(cur, first)
            a.compareAndSet(first, second)
            return value
        }
    }

    private inline fun AtomicInt.extensionLoopRecursive(to: Int): Int {
        loop { cur ->
            compareAndSet(cur, to)
            a.extensionLoop(5)
            return value
        }
    }

    fun testIntExtensionLoops() {
        assertEquals(5, casLoop(5))
        assertEquals(6, casLoopExpression(6))
        assertEquals(66, a.extensionLoop(66))
        assertEquals(77, a.extensionLoopExpression(777))
        assertEquals(99, a.extensionLoopMixedReceivers(88, 99))
        assertEquals(5, a.extensionLoopRecursive(100))
    }

    private inline fun AtomicRef<A>.casLoop(to: String): String = loop { cur ->
        if (compareAndSet(cur, A(to))) {
            val res = value.s
            return "${res}_AtomicRef<A>"
        }
    }

    private inline fun AtomicRef<String>.casLoop(to: String): String = loop { cur ->
        if (compareAndSet(cur, to)) return "${value}_AtomicRef<String>"
    }

    fun testDeclarationWithEqualNames() {
        check(r.casLoop("kk") == "kk_AtomicRef<A>")
        check(rs.casLoop("pp") == "pp_AtomicRef<String>")
    }
}

fun box(): String {
    val testClass = LoopTest()
    testClass.testIntExtensionLoops()
    testClass.testDeclarationWithEqualNames()
    return "OK"
}