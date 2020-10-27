import kotlin.native.concurrent.*
import kotlin.native.internal.GC
import kotlin.test.*

class Holder(var other: Any?)

class Holder2(var field1: Any?, var field2: Any?)

val <T> Array<T>.description: String
    get() {
        val result = StringBuilder()
        result.append('[')
        for (elem in this) {
            result.append(elem.toString())
            result.append(',')
        }
        result.append(']')
        return result.toString()
    }

fun assertArrayEquals(
    expected: Array<Any>,
    actual: Array<Any>
): Unit {
    val lazyMessage: () -> String? = {
        "Expected <${expected.description}>, actual <${actual.description}>."
    }

    asserter.assertTrue(lazyMessage, expected.size == actual.size)
    for (i in expected.indices) {
        asserter.assertTrue(lazyMessage, expected[i] == actual[i])
    }
}

@Test
fun noCycles() {
    val atomic1 = AtomicReference<Any?>(null)
    val atomic2 = AtomicReference<Any?>(null)
    try {
        atomic1.value = atomic2
        val cycles = GC.detectCycles()!!
        assertEquals(0, cycles.size)
        assertNull(GC.findCycle(atomic1));
        assertNull(GC.findCycle(atomic2));
    } finally {
        atomic1.value = null
        atomic2.value = null
    }
}

@Test
fun oneCycle() {
    val atomic = AtomicReference<Any?>(null)
    try {
        atomic.value = atomic
        val cycles = GC.detectCycles()!!
        assertEquals(1, cycles.size)
        assertArrayEquals(arrayOf(atomic, atomic), GC.findCycle(cycles[0])!!)
    } finally {
        atomic.value = null
    }
}

@Test
fun oneCycleWithHolder() {
    val atomic = AtomicReference<Any?>(null)
    try {
        atomic.value = Holder(atomic).freeze()
        val cycles = GC.detectCycles()!!
        assertEquals(1, cycles.size)
        assertArrayEquals(arrayOf(atomic, atomic.value!!, atomic), GC.findCycle(cycles[0])!!)
        assertArrayEquals(arrayOf(atomic.value!!, atomic, atomic.value!!), GC.findCycle(atomic.value!!)!!)
    } finally {
        atomic.value = null
    }
}

@Test
fun oneCycleWithArray() {
    val array = arrayOf(AtomicReference<Any?>(null), AtomicReference<Any?>(null))
    try {
        array[0].value = Holder(array).freeze()
        val cycles = GC.detectCycles()!!
        assertEquals(1, cycles.size)
        assertArrayEquals(arrayOf(array[0], array[0].value!!, array, array[0]), GC.findCycle(cycles[0])!!)
    } finally {
        array[0].value = null
        array[1].value = null
    }
}

@Test
fun oneCycleWithLongChain() {
    val atomic = AtomicReference<Any?>(null)
    try {
        val head = Holder(null)
        var current = head
        repeat(30) {
            val next = Holder(null)
            current.other = next
            current = next
        }
        current.other = atomic
        atomic.value = head.freeze()
        val cycles = GC.detectCycles()!!
        assertEquals(1, cycles.size)
        val cycle = GC.findCycle(cycles[0])!!
        assertEquals(33, cycle.size)
    } finally {
        atomic.value = null
    }
}

@Test
fun twoCycles() {
    val atomic1 = AtomicReference<Any?>(null)
    val atomic2 = AtomicReference<Any?>(null)
    try {
        atomic1.value = atomic2
        atomic2.value = atomic1
        val cycles = GC.detectCycles()!!
        assertEquals(2, cycles.size)
        assertArrayEquals(arrayOf(atomic2, atomic1, atomic2), GC.findCycle(cycles[0])!!)
        assertArrayEquals(arrayOf(atomic1, atomic2, atomic1), GC.findCycle(cycles[1])!!)
    } finally {
        atomic1.value = null
        atomic2.value = null
    }
}

@Test
fun twoCyclesWithHolder() {
    val atomic1 = AtomicReference<Any?>(null)
    val atomic2 = AtomicReference<Any?>(null)
    try {
        atomic1.value = atomic2
        atomic2.value = Holder(atomic1).freeze()
        val cycles = GC.detectCycles()!!
        assertEquals(2, cycles.size)
        assertArrayEquals(arrayOf(atomic2, atomic2.value!!, atomic1, atomic2), GC.findCycle(cycles[0])!!)
        assertArrayEquals(arrayOf(atomic1, atomic2, atomic2.value!!, atomic1), GC.findCycle(cycles[1])!!)
    } finally {
        atomic1.value = null
        atomic2.value = null
    }
}

@Test
fun threeSeparateCycles() {
    val atomic1 = AtomicReference<Any?>(null)
    val atomic2 = AtomicReference<Any?>(null)
    val atomic3 = AtomicReference<Any?>(null)
    try {
        atomic1.value = atomic1
        atomic2.value = Holder2(atomic1, atomic2).freeze()
        atomic3.value = Holder2(atomic3, atomic1).freeze()
        val cycles = GC.detectCycles()!!
        assertEquals(3, cycles.size)
        assertArrayEquals(arrayOf(atomic3, atomic3.value!!, atomic3), GC.findCycle(cycles[0])!!)
        assertArrayEquals(arrayOf(atomic2, atomic2.value!!, atomic2), GC.findCycle(cycles[1])!!)
        assertArrayEquals(arrayOf(atomic1, atomic1), GC.findCycle(cycles[2])!!)
    } finally {
        atomic1.value = null
        atomic2.value = null
        atomic3.value = null
    }
}

@Test
fun noCyclesWithFreezableAtomicReference() {
    val atomic = FreezableAtomicReference<Any?>(null)
    try {
        atomic.value = atomic
        val cycles = GC.detectCycles()!!
        assertEquals(0, cycles.size)
    } finally {
        atomic.value = null
    }
}

@Test
fun oneCycleWithFrozenFreezableAtomicReference() {
    val atomic = FreezableAtomicReference<Any?>(null)
    try {
        atomic.value = atomic
        atomic.freeze()
        val cycles = GC.detectCycles()!!
        assertEquals(1, cycles.size)
        assertArrayEquals(arrayOf(atomic, atomic), GC.findCycle(cycles[0])!!)
    } finally {
        atomic.value = null
    }
}
