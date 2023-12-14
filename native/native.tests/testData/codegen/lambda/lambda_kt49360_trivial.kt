import kotlin.test.*
import kotlin.coroutines.*

// To be tested with -g.

// https://youtrack.jetbrains.com/issue/KT-49360

fun testTrivialCreateBlock(result: Int): () -> Int {
    return (if (result == 0) { { 0 } } else null) ?: { result }
}

fun box(): String {
    assertEquals(0, testTrivialCreateBlock(0)())
    assertEquals(1, testTrivialCreateBlock(1)())

    return "OK"
}
