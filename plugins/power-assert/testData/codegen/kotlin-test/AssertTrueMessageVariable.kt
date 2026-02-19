// FUNCTION: kotlin.test.assertTrue

import kotlin.test.assertTrue

fun box() = expectThrowableMessage {
    val message = "Message:"
    assertTrue(1 != 1, message)
}
