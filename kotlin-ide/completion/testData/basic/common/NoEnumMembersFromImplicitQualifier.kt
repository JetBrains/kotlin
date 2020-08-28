// FIR_COMPARISON
enum class E {
    A
    B
}

fun E.foo() {
    <caret>
}

// ABSENT: A
// ABSENT: B