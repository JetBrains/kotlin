import kotlinx.atomicfu.*
import kotlin.test.*

private val topLevelS = atomic<Any>(arrayOf("A", "B"))

class UncheckedCastTest {
    private val s = atomic<Any>("AAA")
    private val bs = atomic<Any?>(null)

    @Suppress("UNCHECKED_CAST")
    fun testAtomicValUncheckedCast() {
        assertEquals((s as AtomicRef<String>).value, "AAA")
        bs.lazySet(arrayOf(arrayOf(Box(1), Box(2))))
        assertEquals((bs as AtomicRef<Array<Array<Box>>>).value[0]!![0].b * 10, 10)
    }

    @Suppress("UNCHECKED_CAST")
    fun testTopLevelValUnchekedCast() {
        assertEquals((topLevelS as AtomicRef<Array<String>>).value[1], "B")
    }

    private data class Box(val b: Int)

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private inline fun <T> AtomicRef<T>.getString(): String =
        (this as AtomicRef<String>).value

    fun testInlineFunc() {
        assertEquals("AAA", s.getString())
    }

    private val a = atomicArrayOfNulls<Any?>(10)

    fun testArrayValueUncheckedCast() {
        a[0].value = "OK"
        @Suppress("UNCHECKED_CAST")
        assertEquals("OK", (a[0] as AtomicRef<String>).value)
    }

    fun testArrayValueUncheckedCastInlineFunc() {
        a[0].value = "OK"
        assertEquals("OK", a[0].getString())
    }
}

fun box(): String {
    val testClass = UncheckedCastTest()
    testClass.testAtomicValUncheckedCast()
    testClass.testTopLevelValUnchekedCast()
    testClass.testArrayValueUncheckedCast()
    testClass.testArrayValueUncheckedCastInlineFunc()
    testClass.testInlineFunc()
    return "OK"
}