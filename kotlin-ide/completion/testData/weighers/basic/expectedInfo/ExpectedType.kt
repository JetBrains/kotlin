fun<T> foo(p1: Any, p2: Any?, p3: String?): String {
    if (p2 is String) return p<caret>
}

// ORDER: p2
// ORDER: p3
// ORDER: p1
