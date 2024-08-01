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
        assertEquals("B", (topLevelS as AtomicRef<Array<String>>).value[1])
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

    @Suppress("UNCHECKED_CAST")
    fun testAtomicRefUncheckedCastUpdate() {
        bs.lazySet(arrayOf(arrayOf(Box(1), Box(2)), arrayOf(Box(3))))
        (bs as AtomicRef<Array<Array<Box>>>).update { arrayOf(arrayOf(Box(4), Box(5)), arrayOf(Box(6))) }
        assertEquals(5, bs.value[0][1]!!.b)
    }

    @Suppress("UNCHECKED_CAST")
    fun testAtomicRefUncheckedCastGetAndUpdate() {
        bs.lazySet(arrayOf(arrayOf(Box(1), Box(2)), arrayOf(Box(3))))
        val res = (bs as AtomicRef<Array<Array<Box>>>).getAndUpdate { arrayOf(arrayOf(Box(4), Box(5)), arrayOf(Box(6))) }
        assertEquals(2, (res as Array<Array<Box>>)[0][1]!!.b)
        assertEquals(5, bs.value[0][1]!!.b)
    }

    @Suppress("UNCHECKED_CAST")
    fun testAtomicRefUncheckedCastUpdateAndGet() {
        bs.lazySet(arrayOf(arrayOf(Box(1), Box(2)), arrayOf(Box(3))))
        assertEquals(2, (bs as AtomicRef<Array<Array<Box>>>).value[0][1]!!.b)
        val res = (bs as AtomicRef<Array<Array<Box>>>).updateAndGet { arrayOf(arrayOf(Box(4), Box(5)), arrayOf(Box(6))) }
        assertEquals(6, (res as Array<Array<Box>>)[1][0]!!.b)
        assertEquals(6, bs.value[1][0]!!.b)
    }
}

fun box(): String {
    val testClass = UncheckedCastTest()
    testClass.testAtomicValUncheckedCast()
    testClass.testTopLevelValUnchekedCast()
    testClass.testArrayValueUncheckedCast()
    testClass.testArrayValueUncheckedCastInlineFunc()
    testClass.testInlineFunc()
    testClass.testAtomicRefUncheckedCastUpdate()
    testClass.testAtomicRefUncheckedCastGetAndUpdate()
    testClass.testAtomicRefUncheckedCastUpdateAndGet()
    return "OK"
}