fun foo(list: Map<String, Int>, p1: Any, p2: Int) {
    list.containsValue(<caret>)
}

// ABSENT: p1
// EXIST: p2
