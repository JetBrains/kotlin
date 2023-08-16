import kotlinx.atomicfu.*
import kotlin.test.*

class IntArithmetic {
    private val _x = atomic(0)
    val x get() = _x.value

    private val local = atomic(0)

    private fun testGetValue() {
        _x.value = 5
        assertEquals(5, _x.value)
        var aValue = _x.value
        assertEquals(5, aValue)
        assertEquals(5, x)

        local.value = 555
        aValue = local.value
        assertEquals(aValue, local.value)
    }

    private fun testAtomicCallPlaces() {
        _x.value = 5
        _x.compareAndSet(5, 42)
        val res = _x.compareAndSet(42, 45)
        assertTrue(res)
        assertTrue(_x.compareAndSet(45, 77))
        assertFalse(_x.compareAndSet(95, 77))
        assertTrue(_x.compareAndSet(77, 88))
    }

    private fun testInt() {
        _x.value = 0
        assertEquals(0, x)
        val update = 3
        assertEquals(0, _x.getAndSet(update))
        assertTrue(_x.compareAndSet(update, 8))
        _x.lazySet(1)
        assertEquals(1, x)
        assertEquals(1, _x.getAndSet(2))
        assertEquals(2, x)
        assertEquals(2, _x.getAndIncrement())
        assertEquals(3, x)
        assertEquals(3, _x.getAndDecrement())
        assertEquals(2, x)
        assertEquals(2, _x.getAndAdd(2))
        assertEquals(4, x)
        assertEquals(7, _x.addAndGet(3))
        assertEquals(7, x)
        assertEquals(8, _x.incrementAndGet())
        assertEquals(8, x)
        assertEquals(7, _x.decrementAndGet())
        assertEquals(7, x)
        assertTrue(_x.compareAndSet(7, 10))
    }

    fun test() {
        testGetValue()
        testAtomicCallPlaces()
        testInt()
    }
}

class LongArithmetic {
    private val _x = atomic(4294967296)
    val x get() = _x.value
    private val y = atomic(5000000000)
    private val z = atomic(2424920024888888848)
    private val max = atomic(9223372036854775807)

    fun testLong() {
        assertEquals(2424920024888888848, z.value)
        z.lazySet(8424920024888888848)
        assertEquals(8424920024888888848, z.value)
        assertEquals(8424920024888888848, z.getAndSet(8924920024888888848))
        assertEquals(8924920024888888848, z.value)
        assertEquals(8924920024888888849, z.incrementAndGet())
        assertEquals(8924920024888888849, z.value)
        assertEquals(8924920024888888849, z.getAndDecrement())
        assertEquals(8924920024888888848, z.value)
        assertEquals(8924920024888888848, z.getAndAdd(100000000000000000))
        assertEquals(9024920024888888848, z.value)
        assertEquals(-198452011965886959, z.addAndGet(-9223372036854775807))
        assertEquals(-198452011965886959, z.value)
        assertEquals(-198452011965886958, z.incrementAndGet())
        assertEquals(-198452011965886958, z.value)
        assertEquals(-198452011965886959, z.decrementAndGet())
        assertEquals(-198452011965886959, z.value)
    }
}

class BooleanArithmetic {
    private val _x = atomic(false)
    val x get() = _x.value

    fun testBoolean() {
        assertEquals(false, _x.value)
        assertFalse(x)
        _x.lazySet(true)
        assertTrue(x)
        assertTrue(_x.getAndSet(true))
        assertTrue(_x.compareAndSet(true, false))
        assertFalse(x)
    }
}

class ReferenceArithmetic {
    private val _x = atomic<String?>(null)

    fun testReference() {
        _x.value = "aaa"
        assertEquals("aaa", _x.value)
        _x.lazySet("bb")
        assertEquals("bb", _x.value)
        assertEquals("bb", _x.getAndSet("ccc"))
        assertEquals("ccc", _x.value)
    }
}

@Test
fun test() {
    val intClass = IntArithmetic()
    intClass.test()
    val longClass = LongArithmetic()
    longClass.testLong()
    val booleanClass = BooleanArithmetic()
    booleanClass.testBoolean()
    val refClass = ReferenceArithmetic()
    refClass.testReference()
}