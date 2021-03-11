fun foo(p1: Int?, p2: Int) { }

fun bar(p: Int) {
    foo(<caret>)
}

// ELEMENT: null
// CHAR: ','
