// FUNCTION: infix.extension.mustEqual

import infix.extension.*

fun box() = expectThrowableMessage {
    2.mustEqual(6)
}
