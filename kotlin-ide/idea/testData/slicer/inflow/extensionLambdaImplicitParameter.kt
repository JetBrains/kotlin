// FLOW: IN
// RUNTIME_WITH_SOURCES

fun foo(f: String.(Int) -> Unit) {
    f("", 1)

    "".f(2)

    with("") {
        f(3)
    }
}

fun test() {
    foo {
        val v = <caret>it
    }
}

inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    return receiver.block()
}
