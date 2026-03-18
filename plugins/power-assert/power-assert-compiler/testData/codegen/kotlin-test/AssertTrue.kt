// FUNCTION: kotlin.test.assertTrue

import kotlin.test.assertTrue

fun box() = expectThrowableMessage {
    assertTrue(1 != 1)
}
