fun xfoo(option1: String = "", option2: Int = 1, p: (String, Int) -> Unit){}

fun test(param: () -> Unit) {
    xfoo<caret>
}

// ELEMENT: xfoo
// TAIL_TEXT: " { String, Int -> ... } (..., p: (String, Int) -> Unit) (<root>)"

