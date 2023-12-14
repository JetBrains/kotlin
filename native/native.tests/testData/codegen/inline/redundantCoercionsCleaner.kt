import kotlin.test.*

inline fun runAndThrow(action: () -> Unit): Nothing {
    action()
    throw Exception()
}

inline fun foo(): Int = runAndThrow {
    return 1
}

fun box(): String {
    val result: Any = foo()
    assertEquals(1, result)

    return "OK"
}
