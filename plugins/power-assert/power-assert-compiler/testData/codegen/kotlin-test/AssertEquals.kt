// FUNCTION: kotlin.test.assertEquals

import kotlin.test.assertEquals

fun box() = expectThrowableMessage {
    val greeting = "Hello"
    val name = "World"
    assertEquals(greeting, name)
}
