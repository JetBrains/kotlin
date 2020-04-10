// FLOW: IN

fun foo(f: String.(Int) -> Unit) {
    f("", 1)

    "".f(2)

    with("") {
        f(3)
    }
}

fun test() {
    foo { i ->
        val v = <caret>i
    }
}

inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    return receiver.block()
}
