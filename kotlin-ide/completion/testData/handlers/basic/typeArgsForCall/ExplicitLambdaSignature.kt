fun <T> foo(p : (T, Int) -> Boolean){}

fun main(args: Array<String>) {
    fo<caret>
}

// ELEMENT: foo
// TAIL_TEXT: " { T, Int -> ... } (p: (T, Int) -> Boolean) (<root>)"
