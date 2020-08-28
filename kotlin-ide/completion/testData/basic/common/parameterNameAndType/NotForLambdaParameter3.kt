// FIR_COMPARISON
class BBB

fun foo(p: (Int, String) -> Unit) { }

fun bar() {
    foo { a, b<caret> }
}

// NUMBER: 0
