// FUNCTION: infix.dispatch.Wrapper.mustEqual

import infix.dispatch.*

data class Holder<T>(val wrapper: Wrapper<T>)
data object Complex {
    val holder = Holder(Wrapper(3))
}

fun box() = expectThrowableMessage {
    Complex.holder.wrapper mustEqual "world".length
}
