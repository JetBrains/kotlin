fun foo() {
    val aaaB = 1
    <caret>val aaaC = 1
    val aaaD = 1
}

// INVOCATION_COUNT: 1
// EXIST: aaaB
// ABSENT: aaaC, aaaD