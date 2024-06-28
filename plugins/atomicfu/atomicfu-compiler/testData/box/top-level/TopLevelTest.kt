import kotlinx.atomicfu.*
import kotlin.test.*

private val a = atomic(0)
private val b = atomic(2424920024888888848)
private val c = atomic(true)
private val abcNode = atomic(ANode(BNode(CNode(8))))
private val any = atomic<Any?>(null)

internal val a_internal = atomic(0)
internal val b_internal = atomic(2424920024888888848)
internal val c_internal = atomic(true)
internal val abcNode_internal = atomic(ANode(BNode(CNode(8))))
internal val any_internal = atomic<Any?>(null)

private val intArr = AtomicIntArray(3)
private val longArr = AtomicLongArray(5)
private val booleanArr = AtomicBooleanArray(4)
private val refArr = atomicArrayOfNulls<ANode<BNode<CNode>>>(5)
private val anyRefArr = atomicArrayOfNulls<Any>(10)

private val stringAtomicNullArr = atomicArrayOfNulls<String>(10)

class TopLevelPrimitiveTest {

    fun testTopLevelInt() {
        assertEquals(0, a.value)
        assertEquals(0, a.getAndSet(3))
        assertTrue(a.compareAndSet(3, 8))
        a.lazySet(1)
        assertEquals(1, a.value)
        assertEquals(1, a.getAndSet(2))
        assertEquals(2, a.value)
        assertEquals(2, a.getAndIncrement())
        assertEquals(3, a.value)
        assertEquals(3, a.getAndDecrement())
        assertEquals(2, a.value)
        assertEquals(2, a.getAndAdd(2))
        assertEquals(4, a.value)
        assertEquals(7, a.addAndGet(3))
        assertEquals(7, a.value)
        assertEquals(8, a.incrementAndGet())
        assertEquals(8, a.value)
        assertEquals(7, a.decrementAndGet())
        assertEquals(7, a.value)
        assertTrue(a.compareAndSet(7, 10))
    }

    fun testTopLevelLong() {
        assertEquals(2424920024888888848, b.value)
        b.lazySet(8424920024888888848)
        assertEquals(8424920024888888848, b.value)
        assertEquals(8424920024888888848, b.getAndSet(8924920024888888848))
        assertEquals(8924920024888888848, b.value)
        assertEquals(8924920024888888849, b.incrementAndGet())
        assertEquals(8924920024888888849, b.value)
        assertEquals(8924920024888888849, b.getAndDecrement())
        assertEquals(8924920024888888848, b.value)
        assertEquals(8924920024888888848, b.getAndAdd(100000000000000000))
        assertEquals(9024920024888888848, b.value)
        assertEquals(-198452011965886959, b.addAndGet(-9223372036854775807))
        assertEquals(-198452011965886959, b.value)
        assertEquals(-198452011965886958, b.incrementAndGet())
        assertEquals(-198452011965886958, b.value)
        assertEquals(-198452011965886959, b.decrementAndGet())
        assertEquals(-198452011965886959, b.value)
    }

    fun testTopLevelBoolean() {
        assertTrue(c.value)
        c.lazySet(false)
        assertFalse(c.value)
        assertTrue(!c.getAndSet(true))
        assertTrue(c.compareAndSet(true, false))
        assertFalse(c.value)
    }

    fun testTopLevelRef() {
        assertEquals(8, abcNode.value.b.c.d)
        val newNode = ANode(BNode(CNode(76)))
        assertEquals(8, abcNode.getAndSet(newNode).b.c.d)
        assertEquals(76, abcNode.value.b.c.d)
        val l = IntArray(4){i -> i}
        any.lazySet(l)
        assertEquals(2, (any.value as IntArray)[2])
    }

    fun testTopLevelArrayOfNulls() {
        assertEquals(null, stringAtomicNullArr[0].value)
        assertTrue(stringAtomicNullArr[0].compareAndSet(null, "aa"))
        stringAtomicNullArr[1].lazySet("aa")
        assertTrue(stringAtomicNullArr[0].value == stringAtomicNullArr[1].value)
    }
}

class TopLevelArrayTest {

    fun testIntArray() {
        assertTrue(intArr[0].compareAndSet(0, 3))
        assertEquals(0, intArr[1].value)
        intArr[0].lazySet(5)
        assertEquals(5, intArr[0].value + intArr[1].value + intArr[2].value)
        assertTrue(intArr[0].compareAndSet(5, 10))
        assertEquals(10, intArr[0].getAndDecrement())
        assertEquals(9, intArr[0].value)
        intArr[2].value = 2
        assertEquals(2, intArr[2].value)
        assertTrue(intArr[2].compareAndSet(2, 34))
        assertEquals(34, intArr[2].value)
    }

    fun testLongArray() {
        longArr[0].value = 2424920024888888848
        assertEquals(2424920024888888848, longArr[0].value)
        longArr[0].lazySet(8424920024888888848)
        assertEquals(8424920024888888848, longArr[0].value)
        val ac = longArr[0].value
        longArr[3].value = ac
        assertEquals(8424920024888888848, longArr[3].getAndSet(8924920024888888848))
        assertEquals(8924920024888888848, longArr[3].value)
        val ac1 = longArr[3].value
        longArr[4].value = ac1
        assertEquals(8924920024888888849, longArr[4].incrementAndGet())
        assertEquals(8924920024888888849, longArr[4].value)
        assertEquals(8924920024888888849, longArr[4].getAndDecrement())
        assertEquals(8924920024888888848, longArr[4].value)
        longArr[4].value = 8924920024888888848
        assertEquals(8924920024888888848, longArr[4].getAndAdd(100000000000000000))
        val ac2 = longArr[4].value
        longArr[1].value = ac2
        assertEquals(9024920024888888848, longArr[1].value)
        assertEquals(-198452011965886959, longArr[1].addAndGet(-9223372036854775807))
        assertEquals(-198452011965886959, longArr[1].value)
        assertEquals(-198452011965886958, longArr[1].incrementAndGet())
        assertEquals(-198452011965886958, longArr[1].value)
        assertEquals(-198452011965886959, longArr[1].decrementAndGet())
        assertEquals(-198452011965886959, longArr[1].value)
    }

    fun testBooleanArray() {
        assertFalse(booleanArr[1].value)
        booleanArr[1].compareAndSet(false, true)
        booleanArr[0].lazySet(true)
        assertFalse(booleanArr[2].getAndSet(true))
        assertTrue(booleanArr[0].value && booleanArr[1].value && booleanArr[2].value)
    }

    @Suppress("UNCHECKED_CAST")
    fun testRefArray() {
        val a2 = ANode(BNode(CNode(2)))
        val a3 = ANode(BNode(CNode(3)))
        refArr[0].value = a2
        assertEquals(2, refArr[0].value!!.b.c.d)
        assertTrue(refArr[0].compareAndSet(a2, a3))
        assertEquals(3, refArr[0].value!!.b.c.d)
        val r0 = refArr[0].value
        refArr[3].value = r0
        assertEquals(3, refArr[3].value!!.b.c.d)
        val a = abcNode.value
        assertTrue(refArr[3].compareAndSet(a3, a))
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
