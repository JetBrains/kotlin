fun foo(s: String){ }

fun bar(p1: String, p2: Int) {
    foo(<caret>x + 2)
}

// EXIST: p1
// ABSENT: p2
