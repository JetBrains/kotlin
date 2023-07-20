import kotlinx.atomicfu.*
import kotlin.test.*

class ArrayInlineFunctionTest {
    private val anyArr = atomicArrayOfNulls<Any?>(5)
    private val refArr = atomicArrayOfNulls<Box>(5)

    private data class Box(val n: Int)

    fun testSetArrayElementValueInLoop() {
        anyArr[0].loop { cur ->
            assertTrue(anyArr[0].compareAndSet(cur, IntArray(5)))
            return
        }
    }

    private fun action(cur: Box?) = cur?.let { Box(cur.n * 10) }

    fun testArrayElementUpdate() {
        refArr[0].lazySet(Box(5))
        refArr[0].update { cur -> cur?.let { Box(cur.n * 10) } }
        assertEquals(refArr[0].value!!.n, 50)
    }

    fun testArrayElementGetAndUpdate() {
        refArr[0].lazySet(Box(5))
        assertEquals(refArr[0].getAndUpdate { cur -> action(cur) }!!.n, 5)
        assertEquals(refArr[0].value!!.n, 50)
    }

    fun testArrayElementUpdateAndGet() {
        refArr[0].lazySet(Box(5))
        assertEquals(refArr[0].updateAndGet { cur -> action(cur) }!!.n, 50)
    }
}

fun box(): String {
    val testClass = ArrayInlineFunctionTest()
    testClass.testSetArrayElementValueInLoop()
    testClass.testArrayElementGetAndUpdate()
    testClass.testArrayElementUpdate()
    testClass.testArrayElementUpdateAndGet()
    return "OK"
}