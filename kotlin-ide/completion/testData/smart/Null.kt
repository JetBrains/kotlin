fun foo(p: String?) {}

fun bar() {
    foo(<caret>)
}

// EXIST: { itemText: "null", attributes: "bold" }
