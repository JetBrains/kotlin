fun <caret>foo(a: Int, c: Int) {

}

fun test() {
    foo(1,
            3)
    foo(1, 3)
    foo(
            1,
            3
    )
}