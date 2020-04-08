fun foo(f: (Int, Int, Int) -> Unit) {
}

fun main() {
    foo<caret>
}

// ELEMENT: foo
// TAIL_TEXT: " { Int, Int, Int -> ... } (f: (Int, Int, Int) -> Unit) (<root>)"