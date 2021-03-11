// FLOW: IN

fun foo(a: Int, b: Int, f: (Int) -> (Int) -> Int): Int {
    return f(a)(b)
}

fun test() {
    val <caret>x = foo(1, 2) { { it } }
}