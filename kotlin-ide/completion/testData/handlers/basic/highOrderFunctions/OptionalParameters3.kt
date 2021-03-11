fun xfoo(p: () -> Unit = {}){}

fun test() {
    xfo<caret>
}

// ELEMENT: xfoo
// TAIL_TEXT: " {...} (p: () -> Unit = ...) (<root>)"

