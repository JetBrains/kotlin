import kotlinx.atomicfu.*
import kotlin.test.*

class LoopTest {
    private val a = atomic(0)
    private val a1 = atomic(1)
    private val b = atomic(true)
    private val l = atomic(5000000000)
    private val r = atomic<A>(A("aaaa"))
    private val rs = atomic<String>("bbbb")

    class A(val s: String)

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

    private inline fun AtomicInt.foo(to: Int): Int {
        loop { cur ->
            if (compareAndSet(cur, to)) return 777
            else return value
        }
    }

    private inline fun AtomicInt.bar(delta: Int): Int {
        return foo(value + delta)
    }

    fun testIntExtensionLoops() {
        assertEquals(5, casLoop(5))
        assertEquals(6, casLoopExpression(6))
        assertEquals(66, a.extensionLoop(66))
        assertEquals(77, a.extensionLoopExpression(777))
        assertEquals(99, a.extensionLoopMixedReceivers(88, 99))
        assertEquals(5, a.extensionLoopRecursive(100))
        assertEquals(777, a.bar(100))
    }
}

private val ref = atomic<String>("aaa")

private inline fun AtomicRef<String>.topLevelExtensionLoop(to: String): String = loop { cur ->
    lazySet(cur + to)
    return value
}

fun testTopLevelExtensionLoop() {
    assertEquals("aaattt", ref.topLevelExtensionLoop("ttt"))
}

fun box(): String {
    val testClass = LoopTest()
    testClass.testIntExtensionLoops()
    testTopLevelExtensionLoop()
    return "OK"
}
