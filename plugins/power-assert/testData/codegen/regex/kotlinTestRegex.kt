// FUNCTION_REGEX: kotlin\.test\.assert.*

import kotlin.test.*

fun box(): String = runAll(
    "assertTrue" to { test1() },
    "assertEquals" to { test2() },
    "assertFalse" to { test3() },
)

fun test1() {
    val booleanValue = false
    assertTrue(booleanValue)
}


fun test2() {
    val a = 3
    val b = 5
    assertEquals(a, b)
}

fun test3() {
    val booleanValue = true
    assertFalse(booleanValue)
}
