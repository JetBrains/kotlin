fun foo(p: Any?) {
    if (p != null) {
        print("not null")
    }
    else {
        return
    }
    <caret>p.hashCode()
}