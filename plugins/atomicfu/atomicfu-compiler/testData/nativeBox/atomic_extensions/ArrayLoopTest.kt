import kotlinx.atomicfu.*
import kotlin.test.*

class AtomicIntArrayAtomicfuInlineFunctionsTest {
    private val intArr = AtomicIntArray(10)

    fun testSetArrayElementValueInLoop() {
        intArr[0].loop { cur ->
            assertTrue(intArr[0].compareAndSet(cur, 555))
            assertEquals(555, intArr[0].value)
            return
        }
    }

    private fun action(cur: Int): Int = cur * 1000

    fun testArrayElementUpdate() {
        intArr[0].lazySet(5)
        intArr[0].update { cur -> action(cur) }
        assertEquals(intArr[0].value, 5000)
    }

    fun testArrayElementGetAndUpdate() {
        intArr[0].lazySet(5)
        assertEquals(intArr[0].getAndUpdate{ cur -> action(cur) }, 5)
        assertEquals(intArr[0].value, 5000)
    }

    fun testArrayElementUpdateAndGet() {
        intArr[0].lazySet(5)
        assertEquals(intArr[0].updateAndGet{ cur -> action(cur) }, 5000)
    }

    fun test() {
        testSetArrayElementValueInLoop()
        testArrayElementUpdate()
        testArrayElementGetAndUpdate()
        testArrayElementUpdateAndGet()
    }
}

class AtomicBooleanArrayAtomicfuInlineFunctionsTest {
    private val booleanArr = AtomicBooleanArray(10)

    fun testSetArrayElementValueInLoop() {
        booleanArr[0].loop { cur ->
            assertTrue(booleanArr[0].compareAndSet(cur, true))
            assertEquals(true, booleanArr[0].value)
            return
        }
    }

    private fun action(cur: Boolean) = !cur

    fun testArrayElementUpdate() {
        booleanArr[0].lazySet(true)
        booleanArr[0].update{ cur -> action(cur) }
        assertEquals(booleanArr[0].value, false)
    }

    fun testArrayElementGetAndUpdate() {
        booleanArr[0].lazySet(true)
        assertEquals(booleanArr[0].getAndUpdate{ cur -> action(cur) }, true)
        assertEquals(booleanArr[0].value, false)
    }

    fun testArrayElementUpdateAndGet() {
        booleanArr[0].lazySet(true)
        assertEquals(booleanArr[0].updateAndGet{ cur -> action(cur) }, false)
    }

    fun test() {
        testSetArrayElementValueInLoop()
        testArrayElementUpdate()
        testArrayElementGetAndUpdate()
        testArrayElementUpdateAndGet()
    }
}

class AtomicArrayAtomicfuInlineFunctionsTest {
    private val anyArr = atomicArrayOfNulls<Any?>(5)
    private val refArr = atomicArrayOfNulls<Box>(5)

    private data class Box(val n: Int)

    fun testSetArrayElementValueInLoop() {
        anyArr[0].loop { cur ->
            assertTrue(anyArr[0].compareAndSet(cur, IntArray(5)))
            return
        }
    }

    private fun action(cur: Box?) = cur?.let { Box(cur.n * 10) }

    fun testArrayElementUpdate() {
        refArr[0].lazySet(Box(5))
        refArr[0].update { cur -> cur?.let { Box(cur.n * 10) } }
        assertEquals(refArr[0].value!!.n, 50)
    }

    fun testArrayElementGetAndUpdate() {
        refArr[0].lazySet(Box(5))
        assertEquals(refArr[0].getAndUpdate { cur -> action(cur) }!!.n, 5)
        assertEquals(refArr[0].value!!.n, 50)
    }

    fun testArrayElementUpdateAndGet() {
        refArr[0].lazySet(Box(5))
        assertEquals(refArr[0].updateAndGet { cur -> action(cur) }!!.n, 50)
    }

    fun test() {
        testSetArrayElementValueInLoop()
        testArrayElementUpdate()
        testArrayElementGetAndUpdate()
        testArrayElementUpdateAndGet()
    }
}



@Test
fun box() {
    AtomicIntArrayAtomicfuInlineFunctionsTest().test()
    AtomicBooleanArrayAtomicfuInlineFunctionsTest().test()
    AtomicArrayAtomicfuInlineFunctionsTest().test()
}