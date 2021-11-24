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

class ArithmeticTest {
    val local = atomic(0)

    fun testGetValue() {
        val a = IntArithmetic()
        a._x.value = 5
        check(a._x.value == 5)
        var aValue = a._x.value
        check(aValue == 5)
        check(a.x == 5)

        local.value = 555
        aValue = local.value
        check(local.value == aValue)
    }

    fun testAtomicCallPlaces(): Boolean {
        val a = IntArithmetic()
        a._x.value = 5
        a._x.compareAndSet(5, 42)
        val res = a._x.compareAndSet(42, 45)
        check(res)
        check(a._x.compareAndSet(45, 77))
        check(!a._x.compareAndSet(95, 77))
        return a._x.compareAndSet(77, 88)
    }

    fun testInt() {
        val a = IntArithmetic()
        check(a.x == 0)
        val update = 3
        check(a._x.getAndSet(update) == 0)
        check(a._x.compareAndSet(update, 8))
        a._x.lazySet(1)
        check(a.x == 1)
        check(a._x.getAndSet(2) == 1)
        check(a.x == 2)
        check(a._x.getAndIncrement() == 2)
        check(a.x == 3)
        check(a._x.getAndDecrement() == 3)
        check(a.x == 2)
        check(a._x.getAndAdd(2) == 2)
        check(a.x == 4)
        check(a._x.addAndGet(3) == 7)
        check(a.x == 7)
        check(a._x.incrementAndGet() == 8)
        check(a.x == 8)
        check(a._x.decrementAndGet() == 7)
        check(a.x == 7)
        check(a._x.compareAndSet(7, 10))
    }

    fun testLong() {
        val a = LongArithmetic()
        check(a.z.value == 2424920024888888848)
        a.z.lazySet(8424920024888888848)
        check(a.z.value == 8424920024888888848)
        check(a.z.getAndSet(8924920024888888848) == 8424920024888888848)
        check(a.z.value == 8924920024888888848)
        check(a.z.incrementAndGet() == 8924920024888888849) // fails
        check(a.z.value == 8924920024888888849)
        check(a.z.getAndDecrement() == 8924920024888888849)
        check(a.z.value == 8924920024888888848)
        check(a.z.getAndAdd(100000000000000000) == 8924920024888888848)
        check(a.z.value == 9024920024888888848)
        check(a.z.addAndGet(-9223372036854775807) == -198452011965886959)
        check(a.z.value == -198452011965886959)
        check(a.z.incrementAndGet() == -198452011965886958)
        check(a.z.value == -198452011965886958)
        check(a.z.decrementAndGet() == -198452011965886959)
        check(a.z.value == -198452011965886959)
    }

    fun testBoolean() {
        val a = BooleanArithmetic()
        check(!a.x)
        a._x.lazySet(true)
        check(a.x)
        check(a._x.getAndSet(true))
        check(a._x.compareAndSet(true, false))
        check(!a.x)
    }

    fun testReference() {
        val a = ReferenceArithmetic()
        a._x.value = "aaa"
        check(a._x.value == "aaa")
        a._x.lazySet("bb")
        check(a._x.value == "bb")
        check(a._x.getAndSet("ccc") == "bb")
        check(a._x.value == "ccc")
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