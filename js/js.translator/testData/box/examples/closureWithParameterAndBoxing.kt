// EXPECTED_REACHABLE_NODES: 489
fun box(): String {
    return if (apply(5) { arg: Int -> arg + 13 } == 18) "OK" else "fail"
}

fun apply(arg: Int, f: (p: Int) -> Int): Int {
    return f(arg)
}
