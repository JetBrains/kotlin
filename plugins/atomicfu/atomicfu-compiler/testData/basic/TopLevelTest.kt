import kotlinx.atomicfu.*
import kotlin.test.*

private val a = atomic(0)
private val b = atomic(2424920024888888848)
private val c = atomic(true)
private val abcNode = atomic(ANode(BNode(CNode(8))))
private val any = atomic<Any?>(null)

private val intArr = AtomicIntArray(3)
private val longArr = AtomicLongArray(5)
private val booleanArr = AtomicBooleanArray(4)
private val refArr = atomicArrayOfNulls<ANode<BNode<CNode>>>(5)
private val anyRefArr = atomicArrayOfNulls<Any>(10)

private val stringAtomicNullArr = atomicArrayOfNulls<String>(10)

class TopLevelPrimitiveTest {

    fun testTopLevelInt() {
        a.value
        check(a.value == 0)
        check(a.getAndSet(3) == 0)
        check(a.compareAndSet(3, 8))
        a.lazySet(1)
        check(a.value == 1)
        check(a.getAndSet(2) == 1)
        check(a.value == 2)
        check(a.getAndIncrement() == 2)
        check(a.value == 3)
        check(a.getAndDecrement() == 3)
        check(a.value == 2)
        check(a.getAndAdd(2) == 2)
        check(a.value == 4)
        check(a.addAndGet(3) == 7)
        check(a.value == 7)
        check(a.incrementAndGet() == 8)
        check(a.value == 8)
        check(a.decrementAndGet() == 7)
        check(a.value == 7)
        a.compareAndSet(7, 10)
    }

    fun testTopLevelLong() {
        check(b.value == 2424920024888888848)
        b.lazySet(8424920024888888848)
        check(b.value == 8424920024888888848)
        check(b.getAndSet(8924920024888888848) == 8424920024888888848)
        check(b.value == 8924920024888888848)
        check(b.incrementAndGet() == 8924920024888888849)
        check(b.value == 8924920024888888849)
        check(b.getAndDecrement() == 8924920024888888849)
        check(b.value == 8924920024888888848)
        check(b.getAndAdd(100000000000000000) == 8924920024888888848)
        check(b.value == 9024920024888888848)
        check(b.addAndGet(-9223372036854775807) == -198452011965886959)
        check(b.value == -198452011965886959)
        check(b.incrementAndGet() == -198452011965886958)
        check(b.value == -198452011965886958)
        check(b.decrementAndGet() == -198452011965886959)
        check(b.value == -198452011965886959)
    }

    fun testTopLevelBoolean() {
        check(c.value)
        c.lazySet(false)
        check(!c.value)
        check(!c.getAndSet(true))
        check(c.compareAndSet(true, false))
        check(!c.value)
    }

    fun testTopLevelRef() {
        check(abcNode.value.b.c.d == 8)
        val newNode = ANode(BNode(CNode(76)))
        check(abcNode.getAndSet(newNode).b.c.d == 8)
        check(abcNode.value.b.c.d == 76)
        val l = IntArray(4){i -> i}
        any.lazySet(l)
        check((any.value as IntArray)[2] == 2)
    }

    fun testTopLevelArrayOfNulls() {
        check(stringAtomicNullArr[0].value == null)
        check(stringAtomicNullArr[0].compareAndSet(null, "aa"))
        stringAtomicNullArr[1].lazySet("aa")
        check(stringAtomicNullArr[0].value == stringAtomicNullArr[1].value)
    }
}

class TopLevelArrayTest {

    fun testIntArray() {
        check(intArr[0].compareAndSet(0, 3))
        check(intArr[1].value == 0)
        intArr[0].lazySet(5)
        check(intArr[0].value + intArr[1].value + intArr[2].value == 5)
        check(intArr[0].compareAndSet(5, 10))
        check(intArr[0].getAndDecrement() == 10)
        check(intArr[0].value == 9)
        intArr[2].value = 2
        check(intArr[2].value == 2)
        check(intArr[2].compareAndSet(2, 34))
        check(intArr[2].value == 34)
    }

    fun testLongArray() {
        longArr[0].value = 2424920024888888848
        check(longArr[0].value == 2424920024888888848)
        longArr[0].lazySet(8424920024888888848)
        check(longArr[0].value == 8424920024888888848)
        val ac = longArr[0].value
        longArr[3].value = ac
        check(longArr[3].getAndSet(8924920024888888848) == 8424920024888888848)
        check(longArr[3].value == 8924920024888888848)
        val ac1 = longArr[3].value
        longArr[4].value = ac1
        check(longArr[4].incrementAndGet() == 8924920024888888849)
        check(longArr[4].value == 8924920024888888849)
        check(longArr[4].getAndDecrement() == 8924920024888888849)
        check(longArr[4].value == 8924920024888888848)
        longArr[4].value = 8924920024888888848
        check(longArr[4].getAndAdd(100000000000000000) == 8924920024888888848)
        val ac2 = longArr[4].value
        longArr[1].value = ac2
        check(longArr[1].value == 9024920024888888848)
        check(longArr[1].addAndGet(-9223372036854775807) == -198452011965886959)
        check(longArr[1].value == -198452011965886959)
        check(longArr[1].incrementAndGet() == -198452011965886958)
        check(longArr[1].value == -198452011965886958)
        check(longArr[1].decrementAndGet() == -198452011965886959)
        check(longArr[1].value == -198452011965886959)
    }

    fun testBooleanArray() {
        check(!booleanArr[1].value)
        booleanArr[1].compareAndSet(false, true)
        booleanArr[0].lazySet(true)
        check(!booleanArr[2].getAndSet(true))
        check(booleanArr[0].value && booleanArr[1].value && booleanArr[2].value)
    }

    @Suppress("UNCHECKED_CAST")
    fun testRefArray() {
        val a2 = ANode(BNode(CNode(2)))
        val a3 = ANode(BNode(CNode(3)))
        refArr[0].value = a2
        check(refArr[0].value!!.b.c.d == 2)
        check(refArr[0].compareAndSet(a2, a3))
        check(refArr[0].value!!.b.c.d == 3)
        val r0 = refArr[0].value
        refArr[3].value = r0
        check(refArr[3].value!!.b.c.d == 3)
        val a = abcNode.value
        check(refArr[3].compareAndSet(a3, a))
    }

}

data class ANode<T>(val b: T)
data class BNode<T>(val c: T)
data class CNode(val d: Int)

fun box(): String {
    val primitiveTest = TopLevelPrimitiveTest()
    primitiveTest.testTopLevelInt()
    primitiveTest.testTopLevelLong()
    primitiveTest.testTopLevelBoolean()
    primitiveTest.testTopLevelRef()
    primitiveTest.testTopLevelArrayOfNulls()

    val arrayTest = TopLevelArrayTest()
    arrayTest.testIntArray()
    arrayTest.testLongArray()
    arrayTest.testBooleanArray()
    arrayTest.testRefArray()
    return "OK"
}