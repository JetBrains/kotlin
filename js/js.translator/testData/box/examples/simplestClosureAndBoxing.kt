// EXPECTED_REACHABLE_NODES: 489
fun box(): String {
    return if (int_invoker({ 7 }) == 7) "OK" else "fail"
}

fun int_invoker(gen: () -> Int): Int {
    return gen()
}
