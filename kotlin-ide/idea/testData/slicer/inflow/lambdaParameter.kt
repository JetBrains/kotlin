// FLOW: IN

fun foo(f: (Int) -> Unit) {
    f(1)
}

fun test() {
    foo { value ->
        val v = <caret>value
    }
}