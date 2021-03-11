val error = "error"
fun foo(p: Any) {
    if (p !is String) {
        print(error)
    }
    println(<caret>p.size)
}