fun foo() =
    2

data class A(val a: Int, val b: Int)

fun test() {
    val (a, b) =
        A()
}

// SET_TRUE: CONTINUATION_INDENT_FOR_EXPRESSION_BODIES
