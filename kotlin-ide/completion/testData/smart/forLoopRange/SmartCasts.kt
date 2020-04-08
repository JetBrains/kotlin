fun foo(p1: Any, p2: Any) {
    if (p1 is String) {
        for (i in <caret>)
    }
}

// EXIST: p1
// ABSENT: p2
