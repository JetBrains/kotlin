fun foo(p: suspend (x: Char, String) -> Unit){}

fun bar() {
    foo { <caret>}
}

// ELEMENT: "x, s ->"
