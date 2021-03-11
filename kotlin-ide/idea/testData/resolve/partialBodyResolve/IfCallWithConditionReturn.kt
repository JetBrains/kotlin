fun foo(p: Any?) {
    if (x(p == null)) {
        print("returned true")
        return
    }
    <caret>p?.hashCode()
}