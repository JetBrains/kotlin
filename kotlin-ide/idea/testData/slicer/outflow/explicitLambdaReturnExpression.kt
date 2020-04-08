// FLOW: OUT

fun foo(f: (Int) -> Int): Int {
    return f(1)
}

fun test() {
    val x = foo {
        if (it < 0) return@foo <caret>0
        it
    }
}