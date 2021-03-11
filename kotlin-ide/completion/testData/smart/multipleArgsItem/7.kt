fun foo(a: Int, b: String, c: String) {}
fun foo(a: Int, b: String) {}

fun bar(b: String, a: Int, c: String) {
    foo(<caret>)
}

// EXIST: "a, b, c"
// EXIST: "a, b"
