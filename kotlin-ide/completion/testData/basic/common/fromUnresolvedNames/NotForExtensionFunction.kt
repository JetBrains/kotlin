// FIR_COMPARISON
// RUN_HIGHLIGHTING_BEFORE

fun foo() {
    unresolvedInFoo()
}

fun String.<caret>

// ABSENT: unresolvedInFoo
