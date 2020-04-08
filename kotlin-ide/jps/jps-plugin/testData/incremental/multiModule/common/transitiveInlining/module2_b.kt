package b

inline fun b(body: () -> Unit) {
    a.a { println("to be inlined into b") }
    body()
}
