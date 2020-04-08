fun foo(p1: Any?, p2: Any) {
    print(p1 ?: p2)
    <caret>p1?.hashCode()
}
