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

    // loop tests

    fun atomicfuIntLoopTest(newValue: Int): Int {
        a.value = 0
        a.loop { cur ->
            if (cur == 10) {
                a.compareAndSet(10, newValue)
                assertEquals(newValue, a.value)
                return a.value
            } else {
                a.incrementAndGet()
            }
        }
    }

    fun atomicfuBooleanLoopTest(newValue: Boolean): Boolean {
        b.value = true
        b.loop { cur ->
            if (cur == false) {
                if (b.compareAndSet(false, newValue)) {
                    assertEquals(newValue, b.value)
                    return b.value
                }
            } else {
                b.value = false
            }
        }
    }

    fun atomicfuLongLoopTest(newValue: Long): Long {
        l.value = 5000000000
        l.loop { cur ->
            if (cur == 5000000010) {
              if (l.compareAndSet(5000000010, newValue)) {
                  assertEquals(newValue, l.value)
                  return l.value
              }
            } else {
                l.incrementAndGet()
            }
        }
    }

    fun atomicfuRefLoopTest(newValue: A): A {
        r.value = A("aaa")
        r.loop { cur ->
            if (cur.s == "bbb") {
                r.compareAndSet(cur, newValue)
                assertEquals(newValue.s, r.value.s)
                return r.value
            } else {
                r.value = A("bbb")
            }
        }
    }

    fun atomicfuLoopTest() {
        assertEquals(777, atomicfuIntLoopTest(777))
        assertFalse(atomicfuBooleanLoopTest(false))
        assertEquals(9000000000, atomicfuLongLoopTest(9000000000))
        assertEquals("bbbb", atomicfuRefLoopTest(A("bbbb")).s)
    }

    // update tests

    fun atomicfuIntUpdateTest() {
        a.value = 0
        a.update { value ->
            if (value < 10) {
                a.incrementAndGet()
            } else {
                Int.MAX_VALUE
            }
        }
        assertEquals(Int.MAX_VALUE, a.value)
    }

    fun atomicfuBooleanUpdateTest() {
        b.value = false
        b.update { value ->
            if (!value) {
                b.compareAndSet(false, true)
            } else {
                true
            }
        }
        assertEquals(true, b.value)
        assertTrue(b.value)
    }

    fun atomicfuLongUpdateTest() {
        l.value = 0L
        l.update { value ->
            if (value < 10) {
                l.incrementAndGet()
            } else {
                Long.MAX_VALUE
            }
        }
        assertEquals(Long.MAX_VALUE, l.value)
    }

    fun atomicfuRefUpdateTest() {
        r.lazySet(A("aaaa"))
        r.update { value ->
            if (value.s == "aaaa") {
                r.value = A("bbbb")
                r.value
            } else {
                A("cccc${value.s}")
            }
        }
        assertEquals("ccccbbbb", r.value.s)
    }

    fun atomicfuUpdateTest() {
        atomicfuIntUpdateTest()
        atomicfuBooleanUpdateTest()
        atomicfuLongUpdateTest()
        atomicfuRefUpdateTest()
    }

    // updateAndGet tests

    fun atomicfuIntUpdateAndGetTest() {
        a.value = 0
        val resInt = a.updateAndGet { value ->
            if (value < 10) {
                a.incrementAndGet()
            } else {
                Int.MAX_VALUE
            }
        }
        assertEquals(Int.MAX_VALUE, resInt)
        assertEquals(Int.MAX_VALUE, a.value)
    }

    fun atomicfuBooleanUpdateAndGetTest() {
        b.value = false
        val resBool = b.updateAndGet { value ->
            if (!value) {
                b.compareAndSet(false, true)
            } else {
                true
            }
        }
        assertTrue(resBool)
        assertTrue(b.value)
    }

    fun atomicfuLongUpdateAndGetTest() {
        l.value = 0L
        val resLong = l.updateAndGet { value ->
            if (value < 10) {
                l.incrementAndGet()
            } else {
                Long.MAX_VALUE
            }
        }
        assertEquals(Long.MAX_VALUE, l.value)
        assertEquals(Long.MAX_VALUE, resLong)
    }

    fun atomicfuRefUpdateAndGetTest() {
        r.lazySet(A("aaaa"))
        val resRef = r.updateAndGet { value ->
            if (value.s == "aaaa") {
                r.value = A("bbbb")
                r.value
            } else {
                A("cccc${value.s}")
            }
        }
        assertEquals("ccccbbbb", resRef.s)
        assertEquals("ccccbbbb", r.value.s)
    }

    fun atomicfuUpdateAndGetTest() {
        atomicfuIntUpdateAndGetTest()
        atomicfuBooleanUpdateAndGetTest()
        atomicfuLongUpdateAndGetTest()
        atomicfuRefUpdateAndGetTest()
    }

    // getAndUpdate tests

    fun atomicfuIntGetAndUpdateTest() {
        a.value = 0
        val resInt = a.getAndUpdate { value ->
            if (value < 10) {
                a.incrementAndGet()
            } else {
                Int.MAX_VALUE
            }
        }
        assertEquals(10, resInt)
        assertEquals(Int.MAX_VALUE, a.value)
    }

    fun atomicfuBooleanGetAndUpdateTest() {
        b.value = false
        val resBool = b.getAndUpdate { value ->
            if (!value) {
                b.compareAndSet(false, true)
            } else {
                true
            }
        }
        assertTrue(resBool)
        assertTrue(b.value)
    }

    fun atomicfuLongGetAndUpdateTest() {
        l.value = 0L
        val resLong = l.getAndUpdate { value ->
            if (value < 10) {
                l.incrementAndGet()
            } else {
                Long.MAX_VALUE
            }
        }
        assertEquals(Long.MAX_VALUE, l.value)
        assertEquals(10, resLong)
    }

    fun atomicfuRefGetAndUpdateTest() {
        r.lazySet(A("aaaa"))
        val resRef = r.getAndUpdate { value ->
            if (value.s == "aaaa") {
                r.value = A("bbbb")
                r.value
            } else {
                A("cccc${value.s}")
            }
        }
        assertEquals("bbbb", resRef.s)
        assertEquals("ccccbbbb", r.value.s)
    }

    fun atomicfuGetAndUpdateTest() {
        atomicfuIntGetAndUpdateTest()
        atomicfuBooleanGetAndUpdateTest()
        atomicfuLongGetAndUpdateTest()
        atomicfuRefGetAndUpdateTest()
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
