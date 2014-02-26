package foo

fun Any.foo(n: Int): () -> Boolean {
    var count = n
    return { --count >= 0 }
}

fun box(): Boolean {
    return 1.foo(3)() && !1.foo(0)()
}
