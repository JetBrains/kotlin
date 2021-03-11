fun <caret>foo(p1: Int, p2: Int, filter: (Int) -> Boolean, p3: Int = 0){}

fun bar() {
    foo(1, 2, { true })
}