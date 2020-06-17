import kotlinx.atomicfu.*
import kotlin.test.*

class LockFreeLongCounterTest {
    private inline fun testWith(g: LockFreeLongCounter.() -> Long) {
        val c = LockFreeLongCounter()
        check(c.g() == 0L)
        check(c.increment() == 1L)
        check(c.g() == 1L)
        check(c.increment() == 2L)
        check(c.g() == 2L)
    }

    fun testBasic() = testWith { get() }

    fun testGetInner() = testWith { getInner() }

    fun testAdd2() {
        val c = LockFreeLongCounter()
        c.add2()
        check(c.get() == 2L)
        c.add2()
        check(c.get() == 4L)
    }

    fun testSetM2() {
        val c = LockFreeLongCounter()
        c.setM2()
        check(c.get() == -2L)
    }
}

class LockFreeLongCounter {
    private val counter = atomic(0L)

    fun get(): Long = counter.value

    fun increment(): Long = counter.incrementAndGet()

    fun add2() = counter.getAndAdd(2)

    fun setM2() {
        counter.value = -2L // LDC instruction here
    }

    fun getInner(): Long = Inner().getFromOuter()

    // testing how an inner class can get access to it
    private inner class Inner {
        fun getFromOuter(): Long = counter.value
    }
}

fun box(): String {
    val testClass = LockFreeLongCounterTest()
    testClass.testBasic()
    testClass.testAdd2()
    testClass.testSetM2()
    testClass.testGetInner()
    return "OK"
}