// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlin-atomicfu-compiler-plugin-1.9.255-SNAPSHOT-atomicfu-1.jar

import kotlinx.atomicfu.*
import kotlin.test.*

class InitBlockInitializationTest {
    val a: AtomicInt
    val a1: AtomicInt
    val b: AtomicBoolean
    val l: AtomicLong
    val r: AtomicRef<A>
    val rs: AtomicRef<String>

    init {
        a = atomic(0)
        a1 = atomic(1)
        b = atomic(true)
        l = atomic(5000000000)
        r = atomic<A>(A("aaaa"))
        rs = atomic<String>("bbbb")
    }

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
        return if (compareAndSet(cur, to)) value + 1 else incrementAndGet()
    }

    private inline fun AtomicInt.extensionLoopMixedReceivers(first: Int, second: Int): Int {
        loop { cur ->
            compareAndSet(cur, first)
            compareAndSet(first, second)
            return value
        }
    }

    private inline fun AtomicInt.extensionLoopRecursive(to: Int): Int {
        loop { cur ->
            compareAndSet(cur, to)
            extensionLoop(5)
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

    inline fun AtomicInt.extensionEmbeddedLoops(to: Int): Int =
        loop { cur1 ->
            compareAndSet(value, to)
            loop { cur2 ->
                return cur2
            }
        }

    fun testIntExtensionLoops() {
        a.compareAndSet(0, 56)
        assertEquals(56, a.value)
        assertEquals(5, casLoop(5))
        assertEquals(45, a.extensionEmbeddedLoops(45))
        assertEquals(6, casLoopExpression(6))
        assertEquals(17, a.extensionLoopExpression(777))
        assertEquals(66, a.extensionLoop(66))
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

@Test
fun testInitBlockInitialization() {
    val testClass = InitBlockInitializationTest()
    testClass.testIntExtensionLoops()
    testTopLevelExtensionLoop()
}
