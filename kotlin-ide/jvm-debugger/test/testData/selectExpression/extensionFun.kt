fun foo() {
    1.<caret>foo()
}

fun Int.foo() = 1

// EXPECTED: 1.foo()