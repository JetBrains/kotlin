package codegen.inline.redundantCoercionsCleaner

import kotlin.test.*

inline fun runAndThrow(action: () -> Unit): Nothing {
    action()
    throw Exception()
}

inline fun foo(): Int = runAndThrow {
    return 1
}

@Test fun runTest() {
    val result: Any = foo()
    assertEquals(1, result)
}
