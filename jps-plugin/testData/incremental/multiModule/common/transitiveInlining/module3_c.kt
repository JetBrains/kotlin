package c

inline fun c(body: () -> Unit) {
    b.b { println("to be inlined into b") }
    body()
}
