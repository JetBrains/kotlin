fun test(a: IntArray) {
    foo(a.spread<caret>)
}

fun foo(vararg args: Int) {}