// EXPECTED_REACHABLE_NODES: 992
fun box(): String {
    return apply("OK", { arg: String -> arg })
}

fun apply(arg: String, f: (p: String) -> String): String {
    return f(arg)
}
