// FUNCTION: kotlin.test.assertFalse

import kotlin.test.assertFalse

fun box() = expectThrowableMessage {
    assertFalse(1 == 1, "Message:")
}
