// EXPECTED_REACHABLE_NODES: 1279
// Snippet from stdlib test test.exceptions.ExceptionTest

private val cause = Exception("cause")

fun assertSame(x: Any?, y: Any?) {
    if (x !== y) {
        error("Assertion failed")
    }
}

private fun <T : Throwable> testCreateException(
    noarg: () -> T,
    fromMessage: (String?) -> T,
    fromCause: ((Throwable?) -> T)? = null,
    fromMessageCause: ((String?, Throwable?) -> T)? = null
) {
    noarg().let { e ->
        assertEquals(null, e.message)
        assertEquals(null, e.cause)
    }

    fromMessage("message").let { e ->
        assertEquals("message", e.message)
        assertEquals(null, e.cause)
    }

    fromMessage(null).let { e ->
        assertTrue(e.message == null || e.message == "null")
    }

    fromMessageCause?.run {
        invoke("message", cause).let { e ->
            assertEquals("message", e.message)
            assertSame(cause, e.cause)
        }
        invoke(null, null).let { e ->
            assertEquals(null, e.message)
            assertEquals(null, e.cause)
        }
    }

    fromCause?.invoke(cause)?.let { e ->
        assertSame(cause, e.cause)
    }
}

fun box(): String {
    testCreateException(::Throwable, ::Throwable, ::Throwable, ::Throwable)
    return "OK"
}
