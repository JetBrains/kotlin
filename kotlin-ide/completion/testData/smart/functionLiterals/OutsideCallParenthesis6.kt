fun foo(p: Int, handler: ((String, Char) -> Unit)?){}

fun bar(handler: (String, Char) -> Unit) {
    foo(1)<caret>
}

// EXIST: "{ s, c -> ... }"
// EXIST: "{ s: String, c: Char -> ... }"
// ABSENT: null
// ABSENT: handler