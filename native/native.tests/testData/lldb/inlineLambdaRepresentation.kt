// KIND: STANDALONE_LLDB
// FIR_IDENTICAL

inline fun foo(action: () -> Unit) {
    return action()
}

fun bar() = 24

fun main(args: Array<String>) {
    foo {
        bar()
    }
}
