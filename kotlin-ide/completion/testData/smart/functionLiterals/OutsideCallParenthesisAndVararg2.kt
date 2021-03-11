fun foo(vararg args: Int, handler: (String, Char) -> Unit){}

fun bar(handler: (String, Char) -> Unit) {
    foo(1, 2) <caret>
}

// EXIST: "{ s, c -> ... }"
// EXIST: "{ s: String, c: Char -> ... }"
// ABSENT: handler