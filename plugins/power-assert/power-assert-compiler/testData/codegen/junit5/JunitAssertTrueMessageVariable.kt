// FUNCTION: org.junit.jupiter.api.Assertions.assertTrue
// WITH_JUNIT5

import org.junit.jupiter.api.Assertions.assertTrue

fun box() = expectThrowableMessage {
    val message = "Message:"
    assertTrue(1 != 1, message)
}
