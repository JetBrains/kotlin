class A(
        val x: Int,
        val y: String
)

fun foo() {
    A(23, "foo")
}

// LINES(JS):    1 2 3 * 6 8 7 7
// LINES(JS_IR): 2 2 3 3 * 2 2 * 3 3 * 7 7
