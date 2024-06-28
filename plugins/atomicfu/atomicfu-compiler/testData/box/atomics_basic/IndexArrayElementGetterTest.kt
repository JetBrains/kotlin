import kotlinx.atomicfu.*
import kotlin.test.*

class IndexArrayElementGetterTest {
    private val clazz = AtomicArrayClass()

    fun fib(a: Int): Int = if (a == 0 || a == 1) a else fib(a - 1) + fib(a - 2)

    fun testIndexArrayElementGetting() {
        clazz.intArr[8].value = 3
        val i = fib(4)
        val j = fib(5)
        assertEquals(3, clazz.intArr[i + j].value)
        assertEquals(3, clazz.intArr[fib(4) + fib(5)].value)
        clazz.longArr[3].value = 100
        assertEquals(100, clazz.longArr[fib(6) - fib(5)].value)
        assertEquals(100, clazz.longArr[(fib(6) + fib(4)) % 8].value)
        assertEquals(100, clazz.longArr[(fib(6) + fib(4)) % 8].value)
        assertEquals(100, clazz.longArr[(fib(4) + fib(5)) % fib(5)].value)
    }

    private class AtomicArrayClass {
        val intArr = AtomicIntArray(10)
        val longArr = AtomicLongArray(10)
    }
}

fun box(): String {
    val testClass = IndexArrayElementGetterTest()
    testClass.testIndexArrayElementGetting()
    return "OK"
}
