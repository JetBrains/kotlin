fun myError(): Nothing = throw Exception()

fun foo(p: Any) {
    if (p !is String) {
        myError()
    }
    println(<caret>p.length())
}