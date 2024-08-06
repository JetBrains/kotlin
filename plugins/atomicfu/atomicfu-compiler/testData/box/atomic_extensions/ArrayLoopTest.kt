// TODO(KT-65977): reenable these tests with caches
//IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
//IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
import kotlinx.atomicfu.*
import kotlin.test.*

class ArrayLoopTest {
    private val SIZE = 10
    private val intArr = AtomicIntArray(10)
    private val longArr = AtomicLongArray(10)
    private val boolArr = AtomicBooleanArray(10)
    private val refArr = atomicArrayOfNulls<Box?>(10)

    class Box(val n: Int)

    fun atomicIntArrLoopTest(): Int {
        intArr[0].value = 0
        intArr[0].loop { value ->
            if (intArr[0].compareAndSet(value, value + 10)) {
                assertEquals(value + 10, intArr[0].value)
                return intArr[0].value
            }
        }
    }

    fun atomicIntArrUpdateTest() {
        assertEquals(10, atomicIntArrLoopTest())
        intArr[5].value = 10
        intArr[5].update { value ->
            val newValue = value + 1000
            if (newValue >= 0) Int.MAX_VALUE else newValue
        }
        assertEquals(Int.MAX_VALUE, intArr[5].value)

        intArr[6].value = 10
        val res1 = intArr[6].updateAndGet { value ->
            if (value >= 0) Int.MAX_VALUE else value
        }
        assertEquals(Int.MAX_VALUE, res1)

        intArr[7].lazySet(50)
        assertEquals(50, intArr[7].getAndUpdate { value ->
            assertEquals(50, value)
            if (value >= 0) Int.MAX_VALUE else value
        })
        assertEquals(Int.MAX_VALUE, intArr[7].value)
    }

    fun atomicBooleanArrLoopTest(): Boolean {
        boolArr[0].value = false
        boolArr[0].loop { value ->
            if (!value && boolArr[0].compareAndSet(value, true)) {
                assertEquals(true, boolArr[0].value)
                return boolArr[0].value
            }
        }
    }

    fun atomicBooleanArrUpdateTest() {
        assertTrue(atomicBooleanArrLoopTest())

        boolArr[5].update { true }
        assertEquals(true, boolArr[5].value)
        assertTrue(boolArr[5].value)

        assertFalse(boolArr[5].updateAndGet { false })
        assertFalse(boolArr[5].value)

        boolArr[5].lazySet(false)
        assertFalse(boolArr[5].getAndUpdate { true })
        assertTrue(boolArr[5].value)
        assertTrue(boolArr[5].getAndUpdate { cur ->
            assertTrue(cur)
            false
        })
        assertFalse(boolArr[5].value)
    }

    fun atomicLongArrLoopTest(): Long {
        longArr[0].value = 0
        longArr[0].loop { value ->
            if (longArr[0].compareAndSet(value, value + 10)) {
                assertEquals(value + 10, longArr[0].value)
                return longArr[0].value
            }
        }
    }

    fun atomicLongArrUpdateTest() {
        assertEquals(10L, atomicLongArrLoopTest())

        longArr[5].value = 0L
        longArr[5].update { cur ->
            val newValue = cur + 1000
            if (newValue >= 0L) Long.MAX_VALUE else newValue
        }
        assertEquals(Long.MAX_VALUE, longArr[5].value)

        longArr[6].value = 10L
        val res2 = longArr[6].updateAndGet { cur ->
            if (cur >= 0L) Long.MAX_VALUE else cur
        }
        assertEquals(Long.MAX_VALUE, res2)

        longArr[7].lazySet(50)
        assertEquals(50L, longArr[7].getAndUpdate { cur ->
            assertEquals(50L, cur)
            if (cur >= 0L) Long.MAX_VALUE else cur
        })
        assertEquals(Long.MAX_VALUE, longArr[7].value)
    }

    private fun action(cur: Box?) = cur?.let { Box(cur.n * 10) }

    fun atomicRefArrLoopTest(): Box? {
        refArr[0].value = Box(888)
        refArr[0].loop { value ->
            if (refArr[0].compareAndSet(value, Box(777))) {
                assertEquals(777, refArr[0].value!!.n)
                return refArr[0].value
            }
        }
    }

    fun atomicRefArrUpdateTest() {
        assertEquals(777, atomicRefArrLoopTest()?.n)

        refArr[0].lazySet(Box(5))
        refArr[0].update { cur -> cur?.let { Box(cur.n * 10) } }
        assertEquals(refArr[0].value!!.n, 50)

        refArr[0].lazySet(Box(5))
        assertEquals(refArr[0].updateAndGet { cur -> action(cur) }!!.n, 50)

        refArr[0].lazySet(Box(5))
        assertEquals(refArr[0].getAndUpdate { cur -> action(cur) }!!.n, 5)
        assertEquals(refArr[0].value!!.n, 50)
    }
}

fun box(): String {
    val testClass = ArrayLoopTest()
    testClass.atomicIntArrUpdateTest()
    testClass.atomicIntArrUpdateTest()
    testClass.atomicBooleanArrUpdateTest()
    testClass.atomicRefArrUpdateTest()
    return "OK"
}
