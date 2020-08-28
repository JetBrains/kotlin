// FIR_COMPARISON
fun foo(p1: Int, p2: Int) {
}

fun bar(b: Boolean) {
    foo(if (b) 1 else <caret>)
}

// ABSENT: p1
// ABSENT: p2
