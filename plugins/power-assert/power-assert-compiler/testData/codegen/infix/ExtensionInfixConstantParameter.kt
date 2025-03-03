// FUNCTION: infix.extension.mustEqual

import infix.extension.*

fun box() = expectThrowableMessage {
    (1 + 1) mustEqual 6
}
