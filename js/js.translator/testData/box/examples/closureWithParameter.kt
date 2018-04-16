// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1110
fun box(): String {
    return apply("OK", { arg: String -> arg })
}

fun apply(arg: String, f: (p: String) -> String): String {
    return f(arg)
}
