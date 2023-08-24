import kotlinx.atomicfu.*
import kotlin.test.*

private val topLevelA = atomic(0)

class ComplexLoopTest {
    private val a = atomic(10)
    private val long = atomic(6757L)
    private val b = atomic(11)
    private val c = atomic(12)
    private val r = atomic<String>("aaa")
    private val intArr = AtomicIntArray(10)

    private inline fun AtomicInt.fooInt() {
        loop { cur ->
            if (compareAndSet(cur, 67)) return
        }
    }

    private inline fun AtomicLong.fooLong() {
        loop { cur ->
            if (compareAndSet(cur, 67L)) return
        }
    }

    private inline fun embeddedLoops(to: Int): Int =
        a.loop { aValue ->
            if (!a.compareAndSet(aValue, to)) return 666
            b.loop { bValue ->
                return if (b.compareAndSet(bValue, to)) a.value + b.value else 777
            }
        }

    private inline fun AtomicInt.extensionEmbeddedLoops(to: Int): Int =
        loop { cur1 ->
            compareAndSet(cur1, to)
            loop { cur2 ->
                return cur2
            }
        }

    private inline fun embeddedUpdate(to: Int): Int =
        a.loop { aValue ->
            a.compareAndSet(aValue, to)
            return a.updateAndGet { cur -> cur + 100 }
        }

    private inline fun AtomicRef<String>.extesntionEmbeddedRefUpdate(to: String): String =
        loop { value ->
            compareAndSet(value, to)
            return updateAndGet { cur -> "${cur}AAA" }
        }

    fun test() {
        a.fooInt()
        assertEquals(67, a.value)
        b.fooInt()
        assertEquals(67, b.value)
        c.fooInt()
        assertEquals(67, c.value)
        long.fooLong()
        assertEquals(67L, long.value)
        assertEquals(24, embeddedLoops(12))
        assertEquals(77, c.extensionEmbeddedLoops(77))
        assertEquals(66, intArr[0].extensionEmbeddedLoops(66))
        assertEquals(166, embeddedUpdate(66))
        assertEquals("bbbAAA", r.extesntionEmbeddedRefUpdate("bbb"))
    }
}

@Test
fun testComplexLoopTest() {
    val testClass = ComplexLoopTest()
    testClass.test()
}
