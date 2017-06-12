// EXPECTED_REACHABLE_NODES: 489
fun box(): String {
    return invoker({ "OK" })
}

fun invoker(gen: () -> String): String {
    return gen()
}
