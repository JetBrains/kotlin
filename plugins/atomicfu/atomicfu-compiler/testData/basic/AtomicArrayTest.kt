import kotlinx.atomicfu.*
import kotlin.test.*

class AtomicArrayTest {

    fun testIntArray() {
        val A = AtomicArrayClass()
        check(A.intArr[0].compareAndSet(0, 3))
        check(A.intArr[1].value == 0)
        A.intArr[0].lazySet(5)
        check(A.intArr[0].value + A.intArr[1].value + A.intArr[2].value == 5)
        check(A.intArr[0].compareAndSet(5, 10))
        check(A.intArr[0].getAndDecrement() == 10)
        check(A.intArr[0].value == 9)
        A.intArr[2].value = 2
        check(A.intArr[2].value == 2)
        check(A.intArr[2].compareAndSet(2, 34))
        check(A.intArr[2].value == 34)
    }


    fun testLongArray() {
        val A = AtomicArrayClass()
        A.longArr[0].value = 2424920024888888848
        check(A.longArr[0].value == 2424920024888888848)
        A.longArr[0].lazySet(8424920024888888848)
        check(A.longArr[0].value == 8424920024888888848)
        val ac = A.longArr[0].value
        A.longArr[3].value = ac
        check(A.longArr[3].getAndSet(8924920024888888848) == 8424920024888888848)
        check(A.longArr[3].value == 8924920024888888848)
        val ac1 = A.longArr[3].value
        A.longArr[4].value = ac1
        check(A.longArr[4].incrementAndGet() == 8924920024888888849)
        check(A.longArr[4].value == 8924920024888888849)
        check(A.longArr[4].getAndDecrement() == 8924920024888888849)
        check(A.longArr[4].value == 8924920024888888848)
        A.longArr[4].value = 8924920024888888848
        check(A.longArr[4].getAndAdd(100000000000000000) == 8924920024888888848)
        val ac2 = A.longArr[4].value
        A.longArr[1].value = ac2
        check(A.longArr[1].value == 9024920024888888848)
        check(A.longArr[1].addAndGet(-9223372036854775807) == -198452011965886959)
        check(A.longArr[1].value == -198452011965886959)
        check(A.longArr[1].incrementAndGet() == -198452011965886958)
        check(A.longArr[1].value == -198452011965886958)
        check(A.longArr[1].decrementAndGet() == -198452011965886959)
        check(A.longArr[1].value == -198452011965886959)
    }


    fun testBooleanArray() {
        val A = AtomicArrayClass()
        check(!A.booleanArr[1].value)
        A.booleanArr[1].compareAndSet(false, true)
        A.booleanArr[0].lazySet(true)
        check(!A.booleanArr[2].getAndSet(true))
        check(A.booleanArr[0].value && A.booleanArr[1].value && A.booleanArr[2].value)
        A.booleanArr[0].value = false
        check(!A.booleanArr[0].value)
    }

    fun testRefArray() {
        val A = AtomicArrayClass()
        val a2 = ARef(2)
        val a3 = ARef(3)
        A.refArr[0].value = a2
        check(A.refArr[0].value!!.n == 2)
        check(A.refArr[0].compareAndSet(a2, a3))
        check(A.refArr[0].value!!.n == 3)
        val r0 = A.refArr[0].value
        A.refArr[3].value = r0
        check(A.refArr[3].value!!.n == 3)
        val a = A.a.value
        check(A.refArr[3].compareAndSet(a3, a))
    }
}

class AtomicArrayClass {
    val intArr = AtomicIntArray(10)
    val longArr = AtomicLongArray(10)
    val booleanArr = AtomicBooleanArray(10)
    val refArr = atomicArrayOfNulls<ARef>(10)
    val anyArr = atomicArrayOfNulls<Any?>(10)
    val a = atomic(ARef(8))
}

data class ARef(val n: Int)

fun box(): String {
    val testClass = AtomicArrayTest()
    testClass.testIntArray()
    testClass.testLongArray()
    testClass.testBooleanArray()
    testClass.testRefArray()
    return "OK"
}