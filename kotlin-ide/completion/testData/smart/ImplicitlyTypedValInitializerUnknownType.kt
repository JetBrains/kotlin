class C

fun foo(p1: String, p2: Int) {
    val v = <caret>
}

// EXIST: p1
// EXIST: p2
// ABSENT: C
