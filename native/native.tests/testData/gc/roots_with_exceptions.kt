// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.runtime.NativeRuntimeApi,kotlin.experimental.ExperimentalNativeApi

// A call that throws has a second exit: the landing pad. The roots of the frame must be known on
// both exits. Here the collection happens in a callee, then the callee throws, and the frame
// that catches the exception uses its objects.

import kotlin.native.NoInline
import kotlin.native.ref.WeakReference
import kotlin.native.runtime.GC
import kotlin.test.*

class Payload(val text: String)

@NoInline
private fun newPayload(suffix: Int): Payload = Payload("payload-" + suffix.toString())

@NoInline
private fun collectAndThrow(message: String): Nothing {
    GC.collect()
    throw IllegalStateException(message)
}

@NoInline
private fun collectAndReturn(): Int {
    GC.collect()
    return 1
}

@Test
fun localIsLiveInCatchBlock() {
    val payload = newPayload(1)
    val weak = WeakReference(payload)

    try {
        collectAndThrow("boom")
    } catch (e: IllegalStateException) {
        GC.collect()
        assertNotNull(weak.value)
        assertEquals("payload-1", payload.text)
    }
}

@Test
fun localIsLiveAfterCatchBlock() {
    val payload = newPayload(2)
    val weak = WeakReference(payload)

    try {
        collectAndThrow("boom")
    } catch (e: IllegalStateException) {
    }
    GC.collect()

    assertNotNull(weak.value)
    assertEquals("payload-2", payload.text)
}

@Test
fun localIsLiveInFinallyBlockOfPropagatingException() {
    val payload = newPayload(3)
    val weak = WeakReference(payload)
    var seenInFinally: String? = null

    try {
        try {
            collectAndThrow("boom")
        } finally {
            GC.collect()
            assertNotNull(weak.value)
            seenInFinally = payload.text
        }
    } catch (e: IllegalStateException) {
    }

    assertEquals("payload-3", seenInFinally)
}

@Test
fun caughtExceptionIsRooted() {
    val caught = try {
        collectAndThrow("message-" + 42.toString())
    } catch (e: IllegalStateException) {
        e
    }
    val weak = WeakReference(caught)

    GC.collect()

    assertNotNull(weak.value)
    assertEquals("message-42", caught.message)
}

@Test
fun rethrowKeepsRootsOfEveryFrame() {
    val outer = newPayload(4)
    val weakOuter = WeakReference(outer)
    var seen = ""

    try {
        val inner = newPayload(5)
        val weakInner = WeakReference(inner)
        try {
            collectAndThrow("first")
        } catch (e: IllegalStateException) {
            GC.collect()
            assertNotNull(weakInner.value)
            throw IllegalArgumentException("second", e)
        } finally {
            seen += inner.text
        }
    } catch (e: IllegalArgumentException) {
        GC.collect()
        assertNotNull(weakOuter.value)
        seen += outer.text
        assertEquals("first", e.cause?.message)
    }

    assertEquals("payload-5payload-4", seen)
}

@NoInline
private fun throwFromDepth(depth: Int, alive: IntArray) {
    val payload = newPayload(depth)
    val weak = WeakReference(payload)
    try {
        if (depth == 0) collectAndThrow("bottom") else throwFromDepth(depth - 1, alive)
    } finally {
        // Runs while the exception propagates, after the collection in the bottom frame.
        if (weak.value != null && payload.text == "payload-" + depth.toString()) alive[0]++
    }
}

@Test
fun everyFrameOfPropagationKeepsItsObject() {
    val alive = IntArray(1)

    assertFailsWith<IllegalStateException> { throwFromDepth(30, alive) }

    assertEquals(31, alive[0])
}

@Test
fun objectCreatedBeforeThrowingLoopIsUsedAfter() {
    val payload = newPayload(6)
    val weak = WeakReference(payload)
    var caught = 0

    repeat(20) {
        try {
            if (it % 2 == 0) collectAndThrow("even") else collectAndReturn()
        } catch (e: IllegalStateException) {
            caught++
        }
        assertNotNull(weak.value)
    }

    assertEquals(10, caught)
    assertEquals("payload-6", payload.text)
}

// Formatting of a stack trace: for every frame the runtime calls `snprintf_with_addr(..., format, ...)`,
// a function with a variable number of arguments. With the delta-main scheme such a call must not be
// turned into a statepoint, which passes the variable arguments in another way than the callee reads
// them: the function crashed or printed garbage (the failure of ExceptionTest).
//
// The test requires a frame line in the form that the function writes:
//   <number> <image> <address> <symbol> + <offset> <file and line, or nothing>
@Test
fun stackTraceFramesAreFormatted() {
    val throwable = try {
        collectAndThrow("trace-message")
    } catch (e: IllegalStateException) {
        e
    }

    val trace = throwable.stackTraceToString()

    val frameLine = Regex("""^\s+at \d+\s+\S+\s+0x[0-9a-fA-F]+ .* \+ -?\d+ """)
    assertTrue(trace.lines().any { frameLine.containsMatchIn(it) }, trace)
}
