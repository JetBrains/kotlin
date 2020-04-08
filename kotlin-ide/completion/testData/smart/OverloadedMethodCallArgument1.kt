val s = ""
val i = 123

fun f(p1: Any, p2: String, p3: Int) {
    foo(<caret>123) // this call is resolved to Foo(Int) but types from all signatures should participate
}

fun foo(p1: String, p2: Any) {
}

fun foo(p1: Int) {
}

// ABSENT: p1
// EXIST: p2
// EXIST: p3
// EXIST: s
// EXIST: i
