fun foo(p: Int, handler: (String, xx: Char) -> Unit){}

fun bar(handler: (String, Char) -> Unit) {
    foo(1)<caret>
}

// WITH_ORDER
// EXIST: "{ s, xx -> ... }"
// EXIST: "{ s: String, xx: Char -> ... }"
// ABSENT: handler