package usage

inline fun inlineUsage(body: () -> Unit) {
    inline.f { println("to be inlined") }
    body()
}
