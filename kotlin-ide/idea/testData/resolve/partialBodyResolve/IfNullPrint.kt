fun foo(p: Any?) {
    if (p == null) {
        print("null")
    }
    <caret>p.hashCode()
}