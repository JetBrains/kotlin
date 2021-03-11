fun foo() {
    val a = 1
    <caret>a foo 1
}

fun Int.foo(i: Int) = 1

// EXPECTED: a