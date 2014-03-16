fun box(): String {
    return invoker({ "OK" })
}

fun invoker(gen: () -> String): String {
    return gen()
}
