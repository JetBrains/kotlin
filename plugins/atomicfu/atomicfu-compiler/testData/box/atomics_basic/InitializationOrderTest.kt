import kotlinx.atomicfu.*
import kotlin.test.*

private class AAA {
    private val _counter = atomic(5L)
    val counterValue: Long get() = _counter.value
    val delegateCounterValue by _counter
    val lateInitInt: AtomicInt
    val intArr: AtomicIntArray

    // test ensures that transformation does not change the order of initialization
    init {
        lateInitInt = atomic(10)
        assertTrue(lateInitInt.compareAndSet(10, 100))
        assertEquals(100, lateInitInt.value)
        intArr = AtomicIntArray(10)
        intArr[0].value = 10
        assertTrue(intArr[0].compareAndSet(10, 100))
        intArr[1].value = 20
    }

    init {
        assertEquals(5L, _counter.value)
        assertEquals(5L,counterValue)
        assertEquals(5L, delegateCounterValue)
        assertEquals(120, intArr[0].value + intArr[1].value)
    }
}

fun box(): String {
    val intClass = AAA()
    return "OK"
}
