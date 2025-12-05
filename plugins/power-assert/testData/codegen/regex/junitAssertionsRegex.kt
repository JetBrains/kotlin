// FUNCTION_REGEX: org\.junit\.jupiter\.api\.Assertions\.assert.*
// WITH_JUNIT5

import org.junit.jupiter.api.Assertions

fun box(): String = runAll(
    "assertTrue" to { test1() },
    "assertEquals" to { test2() },
    "assertFalse" to { test3() },
)

fun test1() {
    val booleanValue = false
    Assertions.assertTrue(booleanValue)
}


fun test2() {
    val a = 3
    val b = 5
    Assertions.assertEquals(a, b)
}

fun test3() {
    val booleanValue = true
    Assertions.assertFalse(booleanValue)
}
