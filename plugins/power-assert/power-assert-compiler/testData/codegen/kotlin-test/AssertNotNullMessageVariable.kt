// FUNCTION: kotlin.test.assertNotNull

import kotlin.test.assertNotNull

fun box() = expectThrowableMessage {
    val name: String? = null
    val message = "Message:"
    assertNotNull(name, message)
}
