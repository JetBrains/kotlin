val aaabbbccc = 1

fun foo() {
    aaa<caret>bbbccc
}

// INVOCATION_COUNT: 1
// EXIST: aaabbbccc