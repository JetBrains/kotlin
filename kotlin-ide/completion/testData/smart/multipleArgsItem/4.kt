fun foo(a: Int, b: Any, c: String?) {}

fun bar(b: String, a: Int, c: String) {
    foo(<caret>)
}

// EXIST: "a, b, c"
