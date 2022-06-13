import kotlinx.atomicfu.*
import kotlinx.atomicfu.locks.*
import kotlin.test.*

class PropertyDeclarationTest {
    private val a: AtomicInt
    private val head: AtomicRef<String>
    private val lateIntArr: AtomicIntArray
    private val lateRefArr: AtomicArray<String?>

    init {
        a = atomic(0)
        head = atomic("AAA")
        lateIntArr = AtomicIntArray(55)
        lateRefArr = atomicArrayOfNulls<String?>(10)
    }

    fun test() {
        assertEquals(0, a.value)
        assertTrue(head.compareAndSet("AAA", "BBB"))
        assertEquals("BBB", head.value)
        assertEquals(0, lateIntArr[35].value)
        assertEquals(null, lateRefArr[5].value)
    }
}

fun box(): String {
    val testClass = PropertyDeclarationTest()
    testClass.test()
    return "OK"
}