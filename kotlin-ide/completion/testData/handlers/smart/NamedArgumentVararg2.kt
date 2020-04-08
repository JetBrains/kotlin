fun foo(param1: String, vararg param2: Int) { }

fun bar(p: Int) {
    foo("", param2 = <caret>)
}

// ELEMENT: p
