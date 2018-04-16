// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1110
fun box(): String {
    return invoker({ "OK" })
}

fun invoker(gen: () -> String): String {
    return gen()
}
