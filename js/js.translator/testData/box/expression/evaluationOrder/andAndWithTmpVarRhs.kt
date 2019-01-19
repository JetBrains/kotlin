// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1515
fun foo(arg: Any): Boolean {
    return arg == "x"
}

fun box(): String {
    val values = listOf(null, "x")
    return if (values[0] == null && foo(values[1]!!)) "OK" else "fail"
}
