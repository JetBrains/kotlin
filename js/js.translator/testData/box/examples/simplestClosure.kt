// EXPECTED_REACHABLE_NODES: 1282
fun box(): String {
    return invoker({ "OK" })
}

fun invoker(gen: () -> String): String {
    return gen()
}
