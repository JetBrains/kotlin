class A(
        val x: Int,
        val y: String
)

fun foo() {
    A(23, "foo")
}

// LINES(JS_IR): 1 2 3 1 2 2 3 3 2 2 2 3 3 3 6 6 7 7
