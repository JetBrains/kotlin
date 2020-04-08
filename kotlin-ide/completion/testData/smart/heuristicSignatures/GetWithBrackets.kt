fun foo(map: Map<String, Int>, p1: Any, p2: String) {
    map[<caret>]
}

// ABSENT: p1
// EXIST: p2
