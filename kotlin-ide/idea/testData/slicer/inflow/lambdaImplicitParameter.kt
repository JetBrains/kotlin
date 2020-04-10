// FLOW: IN

fun foo(f: (Int) -> Unit): Int {
    return f(1)
}

fun test() {
    foo {
        println(<caret>it)
    }
}