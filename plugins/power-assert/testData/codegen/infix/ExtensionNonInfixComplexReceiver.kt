// FUNCTION: infix.extension.mustEqual

import infix.extension.*

fun box() = expectThrowableMessage {
    "hello".substring(1, 4).length.mustEqual("world".length)
}
