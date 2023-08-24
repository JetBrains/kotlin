import kotlinx.atomicfu.*
import kotlin.test.*

class AtomicIntArrayInlineExtensionTest {
    private val intArr = AtomicIntArray(10)
    private val a = atomic(0)

    private inline fun casLoop(to: Int): Int {
        intArr[0].loop { cur ->
            if (intArr[0].compareAndSet(cur, to)) return intArr[0].value
            return 777
        }
    }

    private inline fun casLoopExpression(to: Int): Int = intArr[3].loop { cur ->
        if (intArr[3].compareAndSet(cur, to)) return intArr[3].value
        return 777
    }

    private inline fun AtomicInt.extensionLoop(to: Int): Int {
        loop { cur ->
            if (compareAndSet(cur, to)) return value
            return 777
        }
    }

    private inline fun AtomicInt.extensionLoopExpression(to: Int): Int = loop { cur ->
        lazySet(cur + 10)
        return if (compareAndSet(cur, to)) value else incrementAndGet()
    }

    private inline fun AtomicInt.extensionLoopMixedReceivers(first: Int, second: Int, index: Int): Int {
        loop { cur ->
            compareAndSet(cur, first)
            intArr[index].compareAndSet(first, second)
            return value
        }
    }

    private inline fun AtomicInt.extensionLoopRecursive(to: Int): Int {
        loop { cur ->
            compareAndSet(cur, to)
            a.extensionLoop(5)
            return value
        }
    }

    private inline fun AtomicInt.foo(to: Int): Int {
        loop { cur ->
            if (compareAndSet(cur, to)) return 777
            else return value
        }
    }

    private inline fun AtomicInt.bar(delta: Int): Int {
        return foo(value + delta)
    }

    fun test() {
        assertEquals(5, casLoop(5))
        assertEquals(6, casLoopExpression(6))
        assertEquals(66, intArr[1].extensionLoop(66))
        assertEquals(66, intArr[2].extensionLoop(66))
        assertEquals(77, intArr[1].extensionLoopExpression(777))
        assertEquals(99, intArr[1].extensionLoopMixedReceivers(88, 99, 1))
        assertEquals(100, intArr[1].extensionLoopRecursive(100))
        assertEquals(777, intArr[1].bar(100))
    }
}

class AtomicLongArrayInlineExtensionTest {
    private val longArr = AtomicLongArray(10)
    private val a = atomic(0L)

    private inline fun casLoop(to: Long): Long {
        longArr[0].loop { cur ->
            if (longArr[0].compareAndSet(cur, to)) return longArr[0].value
            return 777L
        }
    }

    private inline fun casLoopExpression(to: Long): Long = longArr[3].loop { cur ->
        if (longArr[3].compareAndSet(cur, to)) return longArr[3].value
        return 777L
    }

    private inline fun AtomicLong.extensionLoop(to: Long): Long {
        loop { cur ->
            if (compareAndSet(cur, to)) return value
            return 777L
        }
    }

    private inline fun AtomicLong.extensionLoopExpression(to: Long): Long = loop { cur ->
        lazySet(cur + 10L)
        return if (compareAndSet(cur, to)) value else incrementAndGet()
    }

    private inline fun AtomicLong.extensionLoopMixedReceivers(first: Long, second: Long, index: Int): Long {
        loop { cur ->
            compareAndSet(cur, first)
            longArr[index].compareAndSet(first, second)
            return value
        }
    }

    private inline fun AtomicLong.extensionLoopRecursive(to: Long): Long {
        loop { cur ->
            compareAndSet(cur, to)
            a.extensionLoop(5L)
            return value
        }
    }

    private inline fun AtomicLong.foo(to: Long): Long {
        loop { cur ->
            if (compareAndSet(cur, to)) return 777L
            else return value
        }
    }

    private inline fun AtomicLong.bar(delta: Long): Long {
        return foo(value + delta)
    }

    fun test() {
        assertEquals(5L, casLoop(5L))
        assertEquals(6L, casLoopExpression(6L))
        assertEquals(66L, longArr[1].extensionLoop(66L))
        assertEquals(66L, longArr[2].extensionLoop(66L))
        assertEquals(77L, longArr[1].extensionLoopExpression(777L))
        assertEquals(99L, longArr[1].extensionLoopMixedReceivers(88L, 99L, 1))
        assertEquals(100L, longArr[1].extensionLoopRecursive(100L))
        assertEquals(777L, longArr[1].bar(100L))
    }
}

class AtomicBooleanArrayInlineExtensionTest {
    private val booleanArr = AtomicBooleanArray(10)

    private inline fun casLoop(to: Boolean): Boolean {
        booleanArr[0].loop { cur ->
            if (booleanArr[0].compareAndSet(cur, to)) return booleanArr[0].value
        }
    }

    private inline fun casLoopExpression(to: Boolean): Boolean = booleanArr[3].loop { cur ->
        if (booleanArr[3].compareAndSet(cur, to)) return booleanArr[3].value
    }

    private inline fun AtomicBoolean.extensionLoop(to: Boolean): Boolean {
        loop { cur ->
            if (compareAndSet(cur, to)) return value
        }
    }

    private inline fun AtomicBoolean.extensionLoopExpression(to: Boolean): Boolean = loop { cur ->
        lazySet(false)
        return if (compareAndSet(cur, to)) value else !value
    }

    private inline fun AtomicBoolean.extensionLoopMixedReceivers(first: Boolean, second: Boolean, index: Int): Boolean {
        loop { cur ->
            compareAndSet(cur, first)
            booleanArr[index].compareAndSet(first, second)
            return value
        }
    }

    fun test() {
        assertEquals(true, casLoop(true))
        assertEquals(true, casLoopExpression(true))
        assertEquals(true, booleanArr[1].extensionLoop(true))
        assertEquals(true, booleanArr[1].extensionLoopExpression(true))
        assertEquals(false, booleanArr[7].extensionLoopMixedReceivers(true, false, 7))
    }
}

class AtomicRefArrayInlineExtensionTest {
    private val refArr = atomicArrayOfNulls<String?>(10)
    private val a = atomic(0L)

    private inline fun casLoop(to: String): String? {
        refArr[0].loop { cur ->
            if (refArr[0].compareAndSet(cur, to)) return refArr[0].value
        }
    }

    private inline fun casLoopExpression(to: String): String? = refArr[3].loop { cur ->
        if (refArr[3].compareAndSet(cur, to)) return refArr[3].value
    }

    private inline fun AtomicRef<String?>.extensionLoop(to: String): String? {
        loop { cur ->
            if (compareAndSet(cur, to)) return value
            else "incorrect"
        }
    }

    private inline fun AtomicRef<String?>.extensionLoopExpression(to: String): String? = loop { cur ->
        lazySet("aaa")
        return if (compareAndSet(cur, to)) value else "CAS_failed"
    }

    private inline fun AtomicRef<String?>.extensionLoopMixedReceivers(first: String, second: String, index: Int): String? {
        loop { cur ->
            compareAndSet(cur, first)
            refArr[index].compareAndSet(first, second)
            return value
        }
    }

    fun test() {
        assertEquals("aaa", casLoop("aaa"))
        assertEquals("bbb", casLoopExpression("bbb"))
        assertEquals("ccc", refArr[1].extensionLoop("ccc"))
        assertEquals("CAS_failed", refArr[1].extensionLoopExpression("ccc"))
        assertEquals("bbb", refArr[7].extensionLoopMixedReceivers("aaa", "bbb", 7))
    }
}

@Test
fun box() {
    AtomicIntArrayInlineExtensionTest().test()
    AtomicLongArrayInlineExtensionTest().test()
    AtomicBooleanArrayInlineExtensionTest().test()
    AtomicRefArrayInlineExtensionTest().test()
}