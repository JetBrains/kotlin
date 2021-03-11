fun <caret>foo(p2: Int, p1: Int, filter: (Int) -> Boolean, p3: Int = 0){}

fun bar() {
    foo(2, 1, { true })
}