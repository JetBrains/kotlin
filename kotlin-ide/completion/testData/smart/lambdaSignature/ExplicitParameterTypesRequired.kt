fun foo(p: (String, Int) -> Unit){}
fun foo(p: (Char, xx: Any) -> Unit){}

fun bar() {
    foo { <caret> }
}

// EXIST: "String, Int ->"
// EXIST: "Char, xx ->"
// ABSENT: "s, i ->"
// ABSENT: "c, xx ->"
