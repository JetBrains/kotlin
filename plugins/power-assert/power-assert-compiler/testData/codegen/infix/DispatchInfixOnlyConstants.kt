// FUNCTION: infix.dispatch.Wrapper.mustEqual

import infix.dispatch.*

fun box() = expectThrowableMessage {
    Wrapper(2) mustEqual 6
}
