// FLOW: IN

fun foo() {
    with("A") {
        val <caret>v = this
    }
}

inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    return receiver.block()
}
