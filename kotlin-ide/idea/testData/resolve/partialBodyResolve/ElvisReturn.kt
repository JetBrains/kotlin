fun foo(p: Any?) {
    print(p ?: return)
    <caret>p.hashCode()
}
