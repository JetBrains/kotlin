fun foo(list: List<String>, p1: Collection<Any>, p2: List<String>) {
    list.containsAll(<caret>)
}

// ABSENT: p1
// EXIST: p2
