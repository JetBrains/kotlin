import kotlinx.atomicfu.*
import kotlin.test.*

private val _topLevelInt = atomic(42)
var topLevelInt: Int by _topLevelInt

private var topLevelVolatile by atomic(56)

class DelegatedProperties {
    // Delegated properties should be declared in the same scope as the original atomic values
    private val _a = atomic(42)
    var a: Int by _a
    private var privateA: Int by _a

    private val _l = atomic(55555555555)
    private var l: Long by _l

    private val _b = atomic(false)
    private var b: Boolean by _b

    private val _ref = atomic(A(B(77)))
    private var ref: A by _ref

    private var vInt by atomic(77)

    private var vLong by atomic(777777777)

    private var vBoolean by atomic(false)

    private var vRef by atomic(A(B(77)))

    class A (val b: B)
    class B (val n: Int)

   fun testDelegatedAtomicInt() {
        assertEquals(42, a)
        assertEquals(42, privateA)
        _a.compareAndSet(42, 56)
        assertEquals(56, a)
        assertEquals(56, privateA)
        a = 77
        _a.compareAndSet(77,  66)
        privateA = 88
        _a.compareAndSet(88,  66)
        assertEquals(66, _a.value)
        assertEquals(66, a)
        assertEquals(66, privateA)

        val aValue = a + privateA
        assertEquals(132, aValue)
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

    fun testDelegatedVariablesFlow() {
        _a.lazySet(55)
        assertEquals(55, _a.value)
        assertEquals(55, a)
        var aValue = a
        assertEquals(55, aValue)
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
        testDelegatedVariablesFlow()
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

//private val _b = atomic("bbb")

//class ScopedDelegatesTest {
//    private val _c = atomic("ccc")
//
//    inner class InnerClass {
//        var a by atomic("aaa")
//        var b by _b
//        var c by _c
//    }
//
//    fun testScopedVolatileProperties() {
//        val clazz = InnerClass()
//        assertEquals("aaa", clazz.a)
//        clazz.a = "aaa1"
//        assertEquals("aaa1", clazz.a)
//
//        assertEquals("bbb", clazz.b)
//        clazz.b = "bbb1"
//        assertEquals("bbb1", clazz.b)
//        assertEquals("bbb1", _b.value)
//        _b.value = "bbb2"
//        assertEquals("bbb2", clazz.b)
//        assertEquals("bbb", clazz.b)
//
//        assertEquals("ccc", clazz.c)
//        clazz.c = "ccc1"
//        assertEquals("ccc1", clazz.c)
//        assertEquals("ccc1", _c.value)
//        _c.value = "ccc2"
//        assertEquals("ccc2", clazz.c)
//    }
//}

fun box(): String {
    val testClass = DelegatedProperties()
    testClass.test()
    testTopLevelDelegatedProperties()
    testTopLevelVolatileProperties()
//    val scopedDelegatesTest = ScopedDelegatesTest()
//    scopedDelegatesTest.testScopedVolatileProperties()
    return "OK"
}
