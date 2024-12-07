// FUNCTION: kotlin.test.assertFalse

import kotlin.test.assertFalse

fun box() = expectThrowableMessage {
    val message = "Message:"
    assertFalse(1 == 1, message)
}
