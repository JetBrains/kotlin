// FIR_COMPARISON
class AAA

fun foo(p: (Int, String) -> Unit) { }

fun bar() {
    foo { a<caret>, b -> }
}

// NUMBER: 0
