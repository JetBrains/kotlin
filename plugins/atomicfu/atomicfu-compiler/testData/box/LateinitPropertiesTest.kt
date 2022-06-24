import kotlinx.atomicfu.*
import kotlinx.atomicfu.locks.*
import kotlin.test.*

class LateinitPropertiesTest {
    private val a: AtomicInt
    private val head: AtomicRef<String>
    private val dataRef: AtomicRef<Data>
    private val lateIntArr: AtomicIntArray
    private val lateRefArr: AtomicArray<String?>

    private class Data(val n: Int)

    init {
        a = atomic(0)
        head = atomic("AAA")
        lateIntArr = AtomicIntArray(55)
        val data = Data(77)
        dataRef = atomic(data)
        val size = 10
        lateRefArr = atomicArrayOfNulls<String?>(size)
    }

    fun test() {
        assertEquals(0, a.value)
        assertTrue(head.compareAndSet("AAA", "BBB"))
        assertEquals("BBB", head.value)
        assertEquals(0, lateIntArr[35].value)
        assertEquals(77, dataRef.value.n)
        assertEquals(null, lateRefArr[5].value)
    }
}

fun box(): String {
    val testClass = LateinitPropertiesTest()
    testClass.test()
    return "OK"
}
