val prop: Nothing
    get() = throw Exception()

fun foo(p: Any) {
    if (p !is String) {
        prop
    }
    println(<caret>p.length())
}