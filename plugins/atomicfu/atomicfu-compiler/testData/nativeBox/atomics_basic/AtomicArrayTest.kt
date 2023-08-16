import kotlinx.atomicfu.*
import kotlin.test.*

class AtomicArrayTest {

    fun testIntArray() {
        val A = AtomicArrayClass()
        assertTrue(A.intArr[0].compareAndSet(0, 3))
        assertEquals(0, A.intArr[1].value)
        A.intArr[0].lazySet(5)
        assertEquals(5, A.intArr[0].value + A.intArr[1].value + A.intArr[2].value)
        assertTrue(A.intArr[0].compareAndSet(5, 10))
        assertEquals(10, A.intArr[0].getAndDecrement())
        assertEquals(9, A.intArr[0].value)
        A.intArr[2].value = 2
        assertEquals(2, A.intArr[2].value)
        assertTrue(A.intArr[2].compareAndSet(2, 34))
        assertEquals(34, A.intArr[2].value)
    }

    fun testLongArray() {
        val A = AtomicArrayClass()
        A.longArr[0].value = 2424920024888888848
        assertEquals(2424920024888888848, A.longArr[0].value)
        A.longArr[0].lazySet(8424920024888888848)
        assertEquals(8424920024888888848, A.longArr[0].value)
        val ac = A.longArr[0].value
        A.longArr[3].value = ac
        assertEquals(8424920024888888848, A.longArr[3].getAndSet(8924920024888888848))
        assertEquals(8924920024888888848, A.longArr[3].value)
        val ac1 = A.longArr[3].value
        A.longArr[4].value = ac1
        assertEquals(8924920024888888849, A.longArr[4].incrementAndGet())
        assertEquals(8924920024888888849, A.longArr[4].value)
        assertEquals(8924920024888888849, A.longArr[4].getAndDecrement())
        assertEquals(8924920024888888848, A.longArr[4].value)
        A.longArr[4].value = 8924920024888888848
        assertEquals(8924920024888888848, A.longArr[4].getAndAdd(100000000000000000))
        val ac2 = A.longArr[4].value
        A.longArr[1].value = ac2
        assertEquals(9024920024888888848, A.longArr[1].value)
        assertEquals(-198452011965886959, A.longArr[1].addAndGet(-9223372036854775807))
        assertEquals(-198452011965886959, A.longArr[1].value)
        assertEquals(-198452011965886958, A.longArr[1].incrementAndGet())
        assertEquals(-198452011965886958, A.longArr[1].value)
        assertEquals(-198452011965886959, A.longArr[1].decrementAndGet())
        assertEquals(-198452011965886959, A.longArr[1].value)
    }

    fun testBooleanArray() {
        val A = AtomicArrayClass()
        assertFalse(A.booleanArr[1].value)
        assertTrue(A.booleanArr[1].compareAndSet(false, true))
        A.booleanArr[0].lazySet(true)
        assertFalse(A.booleanArr[2].getAndSet(true))
        assertTrue(A.booleanArr[0].value && A.booleanArr[1].value && A.booleanArr[2].value)
        A.booleanArr[0].value = false
        assertFalse(A.booleanArr[0].value)
    }

    fun testRefArray() {
        val A = AtomicArrayClass()
        val a2 = ARef(2)
        val a3 = ARef(3)
        A.refArr[0].value = a2
        assertEquals(2, A.refArr[0].value!!.n)
        assertTrue(A.refArr[0].compareAndSet(a2, a3))
        assertEquals(3, A.refArr[0].value!!.n)
        val r0 = A.refArr[0].value
        A.refArr[3].value = r0
        assertEquals(3, A.refArr[3].value!!.n)
        val a = A.a.value
        assertTrue(A.refArr[3].compareAndSet(a3, a))
    }

    fun testAnyArray() {
        val A = AtomicArrayClass()
        val s1 = "aaa"
        val s2 = "bbb"
        A.anyArr[0].value = s1
        assertEquals("aaa", A.anyArr[0].value)
        assertTrue(A.anyArr[0].compareAndSet(s1, s2))
        assertEquals("bbb", A.anyArr[0].value)
        val r0 = A.anyArr[0].value
        A.anyArr[3].value = r0
        assertEquals("bbb", A.anyArr[3].value)
    }
}

private class AtomicArrayClass {
    val intArr = AtomicIntArray(10)
    val longArr = AtomicLongArray(10)
    val booleanArr = AtomicBooleanArray(10)
    val refArr = atomicArrayOfNulls<ARef>(10)
    val anyArr = atomicArrayOfNulls<Any?>(10)
    internal val a = atomic(ARef(8))
}

data class ARef(val n: Int)

@Test
fun test() {
    val testClass = AtomicArrayTest()
    testClass.testIntArray()
    testClass.testLongArray()
    testClass.testBooleanArray()
    testClass.testRefArray()
    testClass.testAnyArray()
}
