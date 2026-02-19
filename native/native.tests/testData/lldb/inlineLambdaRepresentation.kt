// KIND: STANDALONE_LLDB


inline fun foo(action: () -> Unit) {
    return action()
}

fun bar() = 24

fun main(args: Array<String>) {
    foo {
        bar()
    }
}
