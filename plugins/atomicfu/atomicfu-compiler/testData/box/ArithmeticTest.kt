import kotlinx.atomicfu.*
import kotlin.test.*

class IntArithmetic {
    val _x = atomic(0)
    val x get() = _x.value
}

class LongArithmetic {
    val _x = atomic(4294967296)
    val x get() = _x.value
    val y = atomic(5000000000)
    val z = atomic(2424920024888888848)
    val max = atomic(9223372036854775807)
}

class BooleanArithmetic {
    val _x = atomic(false)
    val x get() = _x.value
}

class ReferenceArithmetic {
    val _x = atomic<String?>(null)
}

class VisibilitiesTest {
    val a = atomic(0)
    public val b = atomic(1)
    private val c = atomic(2)
    internal val d = atomic(3)

    fun test() {
        a.lazySet(45)
        b.lazySet(56)
        c.lazySet(46)
        d.lazySet(67)
    }
}

class ArithmeticTest {
    val local = atomic(0)

    fun testGetValue() {
        val a = IntArithmetic()
        a._x.value = 5
        assertEquals(5, a._x.value)
        var aValue = a._x.value
        assertEquals(5, aValue)
        assertEquals(5, a.x)

        local.value = 555
        aValue = local.value
        assertEquals(aValue, local.value)
    }

    fun testAtomicCallPlaces(): Boolean {
        val a = IntArithmetic()
        a._x.value = 5
        a._x.compareAndSet(5, 42)
        val res = a._x.compareAndSet(42, 45)
        assertTrue(res)
        assertTrue(a._x.compareAndSet(45, 77))
        assertFalse(a._x.compareAndSet(95, 77))
        return a._x.compareAndSet(77, 88)
    }

    fun testInt() {
        val a = IntArithmetic()
        assertEquals(0, a.x)
        val update = 3
        assertEquals(0, a._x.getAndSet(update))
        assertTrue(a._x.compareAndSet(update, 8))
        a._x.lazySet(1)
        assertEquals(1, a.x)
        assertEquals(1, a._x.getAndSet(2))
        assertEquals(2, a.x)
        assertEquals(2, a._x.getAndIncrement())
        assertEquals(3, a.x)
        assertEquals(3, a._x.getAndDecrement())
        assertEquals(2, a.x)
        assertEquals(2, a._x.getAndAdd(2))
        assertEquals(4, a.x)
        assertEquals(7, a._x.addAndGet(3))
        assertEquals(7, a.x)
        assertEquals(8, a._x.incrementAndGet())
        assertEquals(8, a.x)
        assertEquals(7, a._x.decrementAndGet())
        assertEquals(7, a.x)
        assertTrue(a._x.compareAndSet(7, 10))
    }

    fun testLong() {
        val a = LongArithmetic()
        assertEquals(2424920024888888848, a.z.value)
        a.z.lazySet(8424920024888888848)
        assertEquals(8424920024888888848, a.z.value)
        assertEquals(8424920024888888848, a.z.getAndSet(8924920024888888848))
        assertEquals(8924920024888888848, a.z.value)
        assertEquals(8924920024888888849, a.z.incrementAndGet())
        assertEquals(8924920024888888849, a.z.value)
        assertEquals(8924920024888888849, a.z.getAndDecrement())
        assertEquals(8924920024888888848, a.z.value)
        assertEquals(8924920024888888848, a.z.getAndAdd(100000000000000000))
        assertEquals(9024920024888888848, a.z.value)
        assertEquals(-198452011965886959, a.z.addAndGet(-9223372036854775807))
        assertEquals(-198452011965886959, a.z.value)
        assertEquals(-198452011965886958, a.z.incrementAndGet())
        assertEquals(-198452011965886958, a.z.value)
        assertEquals(-198452011965886959, a.z.decrementAndGet())
        assertEquals(-198452011965886959, a.z.value)
    }

    fun testBoolean() {
        val a = BooleanArithmetic()
        assertEquals(false, a._x.value)
        assertFalse(a.x)
        a._x.lazySet(true)
        assertTrue(a.x)
        assertTrue(a._x.getAndSet(true))
        assertTrue(a._x.compareAndSet(true, false))
        assertFalse(a.x)
    }

    fun testReference() {
        val a = ReferenceArithmetic()
        a._x.value = "aaa"
        assertEquals("aaa", a._x.value)
        a._x.lazySet("bb")
        assertEquals("bb", a._x.value)
        assertEquals("bb", a._x.getAndSet("ccc"))
        assertEquals("ccc", a._x.value)
    }
}

fun box(): String {
    val testClass = ArithmeticTest()

    testClass.testGetValue()
    if (!testClass.testAtomicCallPlaces()) return "testAtomicCallPlaces: FAILED"

    testClass.testInt()
    testClass.testLong()
    testClass.testBoolean()
    testClass.testReference()
    return "OK"
}