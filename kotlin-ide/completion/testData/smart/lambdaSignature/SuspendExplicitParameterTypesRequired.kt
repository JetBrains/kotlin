fun foo(p: suspend (String, Int) -> Unit){}
fun foo(p: suspend Any.(Char, xx: Any) -> Unit){}

fun bar() {
    foo { <caret> }
}

// EXIST: "String, Int ->"
// EXIST: "Char, xx ->"
// ABSENT: "s, i ->"
// ABSENT: "c, xx ->"
