fun foo(p: Any?) {
    if (p == null) {
        print("null")
    }
    else {
        return
    }
    <caret>p?.hashCode()
}