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

// The test below is inspired by https://youtrack.jetbrains.com/issue/KT-48876.

fun bar2(): Any {
    return foo2()
}

inline fun foo2(): Int {
    return try {
        throw Throwable()
    } catch (e: Throwable) {
        return 2
    }
}

@Test fun runTest2() {
    assertEquals(2, bar2())
}
