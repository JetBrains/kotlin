fun foo(p: Any?) {
    if (p == null) {
        print("null")
        return
    }
    <caret>p.hashCode()
}