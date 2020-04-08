fun foo() {
    <caret>val aaaB = 1
}

// INVOCATION_COUNT: 1
// ABSENT: aaaB