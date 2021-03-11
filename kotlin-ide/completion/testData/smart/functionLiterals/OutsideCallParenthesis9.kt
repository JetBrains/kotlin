fun foo(handler: (String, Char) -> Unit, optional: Int = 0){}

fun bar(handler: (String, Char) -> Unit) {
    foo() <caret>
}

// NUMBER: 0