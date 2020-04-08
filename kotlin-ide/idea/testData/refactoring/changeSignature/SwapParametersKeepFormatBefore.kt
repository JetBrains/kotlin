fun <caret>foo(a: Int, b: Int, c: Int) {

}

fun test() {
    foo(1,
        2,
        3)
    foo(1, 2,
        3)
    foo(
            1,
            2,
            3
    )
}