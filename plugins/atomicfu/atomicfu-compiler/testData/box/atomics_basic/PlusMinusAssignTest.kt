// TODO(KT-65977): reenable these tests with caches
//IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
//IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
import kotlinx.atomicfu.*
import kotlin.test.*

class AtomicPlusAssignTest {
    private val _x = atomic(0)
    val x get() = _x.value

    private val _l = atomic(0L)
    val l get() = _l.value

    init {
        _x += 1100
        _x -= 100
        _l += 1100
        _l -= 100
    }

    private fun testInt() {
        assertEquals(1000, x)
        for (i in 1 .. 1000) {
            _x += 5
        }
        assertEquals(6000, x)
        for (i in 1 .. 1000) {
            _x -= 5
        }
        assertEquals(1000, x)
    }

    private fun testLong() {
        assertEquals(1000, l)
        for (i in 1 .. 1000) {
            _l += 5
        }
        assertEquals(6000, l)
        for (i in 1 .. 1000) {
            _l -= 5
        }
        assertEquals(1000, l)
    }

    fun test() {
        testInt()
        testLong()
    }
}

class AtomicArrayPlusAssignTest {
    private val intArr = AtomicIntArray(10)
    private val longArr = AtomicLongArray(10)

    init {
        intArr[7] += 1100
        intArr[7] -= 100
        longArr[7] += 1100L
        longArr[7] -= 100L
    }

    private fun testInt() {
        assertEquals(1000, intArr[7].value)
        for (i in 1 .. 1000) {
            intArr[7] += 5
        }
        assertEquals(6000, intArr[7].value)
        for (i in 1 .. 1000) {
            intArr[7] -= 5
        }
        assertEquals(1000, intArr[7].value)
    }

    private fun testLong() {
        assertEquals(1000, longArr[7].value)
        for (i in 1 .. 1000) {
            longArr[7] += 5
        }
        assertEquals(6000, longArr[7].value)
        for (i in 1 .. 1000) {
            longArr[7] -= 5
        }
        assertEquals(1000, longArr[7].value)
    }

    fun test() {
        testInt()
        testLong()
    }
}

fun box(): String {
    val atomicClass = AtomicPlusAssignTest()
    atomicClass.test()
    val atomicArrayClass = AtomicArrayPlusAssignTest()
    atomicArrayClass.test()
    return "OK"
}
