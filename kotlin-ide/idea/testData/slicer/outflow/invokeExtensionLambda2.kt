// FLOW: OUT

fun String.foo(<caret>p: String) {
    val v1 = bar(p) { { this } }
}

inline fun bar(x: String, lambda: () -> String.() -> String): String {
    return lambda()(x)
}
