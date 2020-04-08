val s = ""
val i = 123

fun f(p1: Any, p2: String, p3: Int, p4: Char) {
    foo(<caret>abc)
}

fun foo(p1: String, p2: Any) {
}

fun foo(p1: Int) {
}

// add this signature so that call above is not resolved
fun foo(p1: Char) {
}

// ABSENT: p1
// EXIST: p2
// EXIST: p3
// EXIST: p4
// EXIST: s
// EXIST: i
