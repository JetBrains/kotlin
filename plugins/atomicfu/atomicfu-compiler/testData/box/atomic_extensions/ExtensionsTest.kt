import kotlinx.atomicfu.*
import kotlin.test.*

class ExtensionsTest {
    private val a = atomic(0)
    private val l = atomic(0L)
    private val s = atomic<String?>(null)
    private val b = atomic(true)

    fun testScopedFieldGetters() {
        check(a.value == 0)
        val update = 3
        a.lazySet(update)
        check(a.compareAndSet(update, 8))
        a.lazySet(1)
        check(a.value == 1)
        check(a.getAndSet(2) == 1)
        check(a.value == 2)
        check(a.getAndIncrement() == 2)
        check(a.value == 3)
        check(a.getAndDecrement() == 3)
        check(a.value == 2)
        check(a.getAndAdd(2) == 2)
        check(a.value == 4)
        check(a.addAndGet(3) == 7)
        check(a.value == 7)
        check(a.incrementAndGet() == 8)
        check(a.value == 8)
        check(a.decrementAndGet() == 7)
        check(a.value == 7)
        check(a.compareAndSet(7, 10))
    }

    private inline fun AtomicInt.intExtensionArithmetic() {
        value = 0
        check(value == 0)
        val update = 3
        lazySet(update)
        check(compareAndSet(update, 8))
        lazySet(1)
        check(value == 1)
        check(getAndSet(2) == 1)
        check(value == 2)
        check(getAndIncrement() == 2)
        check(value == 3)
        check(getAndDecrement() == 3)
        check(value == 2)
        check(getAndAdd(2) == 2)
        check(value == 4)
        check(addAndGet(3) == 7)
        check(value == 7)
        check(incrementAndGet() == 8)
        check(value == 8)
        check(decrementAndGet() == 7)
        check(value == 7)
        check(compareAndSet(7, 10))
        check(compareAndSet(value, 55))
        check(value == 55)
    }

    private inline fun AtomicLong.longExtensionArithmetic() {
        value = 2424920024888888848
        check(value == 2424920024888888848)
        lazySet(8424920024888888848)
        check(value == 8424920024888888848)
        check(getAndSet(8924920024888888848) == 8424920024888888848)
        check(value == 8924920024888888848)
        check(incrementAndGet() == 8924920024888888849) // fails
        check(value == 8924920024888888849)
        check(getAndDecrement() == 8924920024888888849)
        check(value == 8924920024888888848)
        check(getAndAdd(100000000000000000) == 8924920024888888848)
        check(value == 9024920024888888848)
        check(addAndGet(-9223372036854775807) == -198452011965886959)
        check(value == -198452011965886959)
        check(incrementAndGet() == -198452011965886958)
        check(value == -198452011965886958)
        check(decrementAndGet() == -198452011965886959)
        check(value == -198452011965886959)
    }

    private inline fun AtomicRef<String?>.refExtension() {
        value = "aaa"
        check(value == "aaa")
        lazySet("bb")
        check(value == "bb")
        check(getAndSet("ccc") == "bb")
        check(value == "ccc")
    }

    private inline fun AtomicBoolean.booleanExtensionArithmetic() {
        value = false
        check(!value)
        lazySet(true)
        check(value)
        check(getAndSet(true))
        check(compareAndSet(value, false))
        check(!value)
    }

    fun testExtension() {
        a.intExtensionArithmetic()
        l.longExtensionArithmetic()
        s.refExtension()
        b.booleanExtensionArithmetic()
    }
}


fun box(): String {
    val testClass = ExtensionsTest()
    testClass.testScopedFieldGetters()
    testClass.testExtension()
    return "OK"
}
