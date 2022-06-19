import kotlinx.atomicfu.*
import kotlin.test.*

private val _topLevelInt = atomic(42)
var topLevelInt: Int by _topLevelInt

private var topLevelVolatile by atomic(56)

class DelegatedProperties {
    private val _a = atomic(42)
    var a: Int by _a

    private val _l = atomic(55555555555)
    var l: Long by _l

    private val _b = atomic(false)
    var b: Boolean by _b

    private val _ref = atomic(A(B(77)))
    var ref: A by _ref

    var vInt by atomic(77)

    var vLong by atomic(777777777)

    var vBoolean by atomic(false)

    var vRef by atomic(A(B(77)))

    class A (val b: B)
    class B (val n: Int)

   fun testDelegatedAtomicInt() {
        assertEquals(42, a)
        _a.compareAndSet(42, 56)
        assertEquals(56, a)
        a = 77
        _a.compareAndSet(77,  66)
        assertEquals(66, _a.value)
        assertEquals(66, a)
    }

    fun testDelegatedAtomicLong() {
        assertEquals(55555555555, l)
        _l.getAndIncrement()
        assertEquals(55555555556, l)
        l = 7777777777777
        assertTrue(_l.compareAndSet(7777777777777, 66666666666))
        assertEquals(66666666666, _l.value)
        assertEquals(66666666666, l)
    }

    fun testDelegatedAtomicBoolean() {
        assertEquals(false, b)
        _b.lazySet(true)
        assertEquals(true, b)
        b = false
        assertTrue(_b.compareAndSet(false, true))
        assertEquals(true, _b.value)
        assertEquals(true, b)
    }

    fun testDelegatedAtomicRef() {
        assertEquals(77, ref.b.n)
        _ref.lazySet(A(B(66)))
        assertEquals(66, ref.b.n)
        assertTrue(_ref.compareAndSet(_ref.value, A(B(56))))
        assertEquals(56, ref.b.n)
        ref = A(B(99))
        assertEquals(99, _ref.value.b.n)
    }

    fun testVolatileInt() {
        assertEquals(77, vInt)
        vInt = 55
        assertEquals(110, vInt * 2)
    }

    fun testVolatileLong() {
        assertEquals(777777777, vLong)
        vLong = 55
        assertEquals(55, vLong)
    }

    fun testVolatileBoolean() {
        assertEquals(false, vBoolean)
        vBoolean = true
        assertEquals(true, vBoolean)
    }

    fun testVolatileRef() {
        assertEquals(77, vRef.b.n)
        vRef = A(B(99))
        assertEquals(99, vRef.b.n)
    }

    inner class D {
        var b: Int by _a
        var c by atomic("aaa")
    }

    fun testScopedDelegatedProperties() {
        val clazz = D()
        clazz.b = 42
        _a.compareAndSet(42, 56)
        assertEquals(56, clazz.b)
        clazz.b = 77
        _a.compareAndSet(77, 66)
        assertEquals(66, _a.value)
        assertEquals(66, clazz.b)

        assertEquals("aaa", clazz.c)
        clazz.c = "bbb"
        assertEquals("bbb", clazz.c)
    }

    fun test() {
        testDelegatedAtomicInt()
        testDelegatedAtomicLong()
        testDelegatedAtomicBoolean()
        testDelegatedAtomicRef()
        testVolatileInt()
        testVolatileBoolean()
        testVolatileLong()
        testVolatileRef()
        testScopedDelegatedProperties()
    }
}

fun testTopLevelDelegatedProperties() {
    assertEquals(42, topLevelInt)
    _topLevelInt.compareAndSet(42, 56)
    assertEquals(56, topLevelInt)
    topLevelInt = 77
    _topLevelInt.compareAndSet(77, 66)
    assertEquals(66, _topLevelInt.value)
    assertEquals(66, topLevelInt)
}

fun testTopLevelVolatileProperties() {
    assertEquals(56, topLevelVolatile)
    topLevelVolatile = 55
    assertEquals(110, topLevelVolatile * 2)
}

fun box(): String {
    val testClass = DelegatedProperties()
    testClass.test()
    testTopLevelDelegatedProperties()
    testTopLevelVolatileProperties()
    return "OK"
}
