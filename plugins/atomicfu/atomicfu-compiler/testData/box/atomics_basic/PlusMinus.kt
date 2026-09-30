import kotlinx.atomicfu.*
import kotlin.test.*

class PM {
    private val i = atomic(0)
    private val l = atomic(0L)

    private val iArray = AtomicIntArray(10)
    private val lArray = AtomicLongArray(10)

    fun scalars() {
        i += 3
        assertEquals(3, i.value)
        i -= 4
        assertEquals(-1, i.value)

        l += 42L
        assertEquals(42L, l.value)
        l -= 10L
        assertEquals(32L, l.value)
    }

    fun arrays() {
        iArray[0] += 3
        assertEquals(3, iArray[0].value)
        iArray[0] -= 4
        assertEquals(-1, iArray[0].value)

        lArray[0] += 42L
        assertEquals(42L, lArray[0].value)
        lArray[0] -= 10L
        assertEquals(32L, lArray[0].value)
    }
}

fun box(): String {
    val testClass = PM()
    testClass.scalars()
    testClass.arrays()
    return "OK"
}
