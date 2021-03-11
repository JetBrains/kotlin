fun foo() {
    <caret>bar()
}

fun bar() = 1

// EXPECTED: null