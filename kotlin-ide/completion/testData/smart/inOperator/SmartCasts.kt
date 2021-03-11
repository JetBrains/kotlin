fun foo(s: String, o1: Any, o2: Any) {
    if (o1 is Collection<String> && s in <caret>
}

// EXIST: o1
// ABSENT: o2
