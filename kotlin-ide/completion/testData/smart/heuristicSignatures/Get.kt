fun foo(list: Map<String, Int>, p1: Any, p2: String) {
    list.get(<caret>)
}

// ABSENT: p1
// EXIST: p2
