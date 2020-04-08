// FLOW: IN

fun foo(f: (Int) -> Int): Int {
    return f(1)
}

fun test() {
    fun bar(n: Int) = n
    val <caret>x = foo(::bar)
}