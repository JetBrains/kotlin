fun foo(p: suspend (String, Int) -> Unit){}
fun foo(p: suspend (Char, xx: Any) -> Unit){}

fun bar() {
    foo(<caret>)
}

// ABSENT: "{ s, i -> ... }"
// ABSENT: "{ c, xx -> ... }"
// EXIST: "{ String, Int -> ... }"
// EXIST: "{ Char, xx -> ... }"
