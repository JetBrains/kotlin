// FUNCTION: infix.dispatch.Wrapper.mustEqual

import infix.dispatch.*

fun box() = expectThrowableMessage {
    Wrapper(1 + 1) mustEqual 6
}
