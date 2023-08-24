import kotlinx.atomicfu.*
import kotlin.test.*

class TraceTest {
    private val defaultTrace = Trace()
    private val a1 = atomic(5, defaultTrace)

    private val traceWithSize = Trace(30)
    private val a2 = atomic(1, traceWithSize)

    private val traceWithFormat = Trace(format = TraceFormat { i, text -> "[$i: $text]" })
    private val a = atomic(0, traceWithFormat)

    private val traceWithSizeAndFormat = Trace(30, TraceFormat { id, text -> "$id: $text"})
    private val a3 = atomic(2)

    private val shortTrace = Trace(4)
    private val s = atomic(0, shortTrace.named("s"))

    fun testDefaultTrace() {
        val oldValue = a1.value
        defaultTrace { "before CAS value = $oldValue" }
        val res = a1.compareAndSet(oldValue, oldValue * 10)
        val newValue = a1.value
        defaultTrace { "after CAS value = $newValue" }
    }

    fun testTraceWithSize() {
        val oldValue = a2.value
        traceWithSize { "before CAS value = $oldValue" }
        assertTrue(a2.compareAndSet(oldValue, oldValue * 10))
        traceWithSize { "after CAS value = ${a2.value}" }
        traceWithSize { "before getAndDecrement value = ${a2.value}" }
        a2.getAndDecrement()
        assertEquals(9, a2.value)
        traceWithSize { "after getAndDecrement value = ${a2.value}" }
    }

    fun testTraceWithFormat() {
        val oldValue = a3.value
        traceWithFormat { "before CAS value = $oldValue" }
        assertTrue(a3.compareAndSet(oldValue, oldValue * 10))
        traceWithFormat { "after CAS value = ${a3.value}" }
        traceWithFormat { "before getAndDecrement value = ${a3.value}" }
        a3.getAndDecrement()
        assertEquals(19, a3.value)
        traceWithFormat { "after getAndDecrement value = ${a3.value}" }
    }

    fun testNamedTrace() {
        s.value = 5
        shortTrace { "before CAS value = ${s.value}" }
        s.compareAndSet(5, -2)
        assertEquals(-2, s.value)
        shortTrace { "after CAS value = ${s.value}" }
    }

    private enum class Status { START, END }

    fun testMultipleAppend() {
        val i = 1
        traceWithFormat.append(i, Status.START)
        assertEquals(0, a.value)
        a.incrementAndGet()
        traceWithFormat.append(i, a.value, "incAndGet")
        assertEquals(1, a.value)
        a.lazySet(10)
        traceWithFormat.append(i, a.value, "lazySet")
        assertEquals(10, a.value)
        traceWithFormat.append(i, Status.END)
    }

    fun testTraceInBlock() {
        a1.lazySet(5)
        if (a1.value == 5) {
            defaultTrace { "Value checked" }
            if (a1.compareAndSet(5, 10)) {
                defaultTrace { "CAS succeeded" }
            }
        }
        assertEquals(10, a1.value)
        while (true) {
            if (a1.value == 10) {
                defaultTrace.append("Value checked", a1.value)
                a1.value = 15
                break
            } else {
                defaultTrace.append("Wrong value", a1.value)
            }
        }
    }

    fun test() {
        testDefaultTrace()
        testTraceWithSize()
        testTraceWithFormat()
        testNamedTrace()
        testMultipleAppend()
        testTraceInBlock()
    }
}

@Test
fun box() {
    TraceTest().test()
}