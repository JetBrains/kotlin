fun foo(o: Any) {
    o.<caret>
}

// INVOCATION_COUNT: 0
// ELEMENT: hashCode
// CHAR: .