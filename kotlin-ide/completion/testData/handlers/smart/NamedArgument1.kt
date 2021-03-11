fun foo(param1: String, param2: Int) { }

fun bar(pInt: Int) {
    foo(param2 = <caret>)
}

// ELEMENT: pInt
