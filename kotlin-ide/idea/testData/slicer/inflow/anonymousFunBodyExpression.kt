// FLOW: IN

fun foo(f: (Int) -> Int): Int {
    return f(1)
}

fun test() {
    val <caret>x = foo(fun(n: Int) = n)
}