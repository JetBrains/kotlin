fun foo(p: Int, handler: () -> Unit, optional: String = ""){}

fun bar(p: Int) {
    foo(<caret>)
}

// ELEMENT: p
