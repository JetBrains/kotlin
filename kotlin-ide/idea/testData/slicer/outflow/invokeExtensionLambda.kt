// FLOW: OUT

fun String.foo(<caret>p: String) {
    val v1 = f(p, { this })

    val v2 = g("a", "b", p, { p1, p2 -> p2 })

    val v3 = inlineF(p, { this })
}

fun f(receiver: String, lambda: String.() -> String): String {
    return lambda.invoke(receiver)
}

fun g(a: String, b: String, c: String, lambda: String.(String, String) -> String): String {
    return lambda.invoke(a, b, c)
}

inline fun inlineF(receiver: String, lambda: String.() -> String): String {
    return lambda.invoke(receiver)
}
