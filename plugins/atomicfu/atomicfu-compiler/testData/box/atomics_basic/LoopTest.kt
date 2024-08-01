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

    fun atomicfuIntLoopTest(): Int {
        a.loop { value ->
            if (a.compareAndSet(value, 777)) {
                assertEquals(777, a.value)
                return a.value
            }
        }
    }

    fun atomicfuBooleanLoopTest(): Boolean {
        b.loop { value ->
            assertTrue(value)
            if (b.compareAndSet(value, false)) {
                return b.value
            }
        }
    }

    fun atomicfuLongLoopTest(): Long {
        l.loop { cur ->
            if (l.compareAndSet(5000000003, 9000000000)) {
                return l.value
            } else {
                l.incrementAndGet()
            }
        }
    }

    fun atomicfuRefLoopTest(): A {
        r.loop { cur ->
            assertEquals("aaaa", cur.s)
            if (r.compareAndSet(cur, A("bbbb"))) {
                return r.value
            }
        }
    }

    fun atomicfuLoopTest() {
        assertEquals(777, atomicfuIntLoopTest())
        assertFalse(atomicfuBooleanLoopTest())
        assertEquals(9000000000, atomicfuLongLoopTest())
        assertEquals("bbbb", atomicfuRefLoopTest().s)
    }

    fun atomicfuUpdateTest() {
        a.value = 0
        a.update { value ->
            val newValue = value + 1000
            if (newValue >= 0) Int.MAX_VALUE else newValue
        }
        assertEquals(Int.MAX_VALUE, a.value)
        b.update { true }
        assertEquals(true, b.value)
        assertTrue(b.value)
        l.value = 0L
        l.update { cur ->
            val newValue = cur + 1000
            if (newValue >= 0L) Long.MAX_VALUE else newValue
        }
        assertEquals(Long.MAX_VALUE, l.value)
        r.lazySet(A("aaaa"))
        r.update { cur ->
            A("cccc${cur.s}")
        }
        assertEquals("ccccaaaa", r.value.s)
    }

    fun atomicfuUpdateAndGetTest() {
        val res1 = a.updateAndGet { value ->
            if (value >= 0) Int.MAX_VALUE else value
        }
        assertEquals(Int.MAX_VALUE, res1)
        assertTrue(b.updateAndGet { true })
        val res2 = l.updateAndGet { cur ->
            if (cur >= 0L) Long.MAX_VALUE else cur
        }
        assertEquals(Long.MAX_VALUE, res2)
        r.lazySet(A("aaaa"))
        val res3 = r.updateAndGet { cur ->
            A("cccc${cur.s}")
        }
        assertEquals("ccccaaaa", res3.s)
    }

    fun atomicfuGetAndUpdateTest() {
        a.lazySet(50)
        assertEquals(50, a.getAndUpdate { value ->
            if (value >= 0) Int.MAX_VALUE else value
        })
        assertEquals(Int.MAX_VALUE, a.value)
        b.lazySet(false)
        assertFalse(b.getAndUpdate { true })
        assertTrue(b.value)
        l.lazySet(50)
        assertEquals(50, l.getAndUpdate { cur ->
            if (cur >= 0L) Long.MAX_VALUE else cur
        })
        assertEquals(Long.MAX_VALUE, l.value)
        r.lazySet(A("aaaa"))
        assertEquals("aaaa", r.getAndUpdate { cur -> A("cccc${cur.s}") }.s)
        assertEquals("ccccaaaa", r.value.s)
    }
}

fun box(): String {
    val testClass = LoopTest()
    testClass.atomicfuLoopTest()
    testClass.atomicfuUpdateTest()
    testClass.atomicfuUpdateAndGetTest()
    testClass.atomicfuGetAndUpdateTest()
    return "OK"
}
