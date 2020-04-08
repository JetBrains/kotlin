fun foo(p: Any) {
    if (p !is String) {
        throw IllegalArgumentException()
    }
    println(<caret>p.length())
}