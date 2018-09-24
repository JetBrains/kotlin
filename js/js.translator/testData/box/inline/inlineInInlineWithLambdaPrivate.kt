// EXPECTED_REACHABLE_NODES: 1286
var global = ""

fun log(message: String) {
    global += message + ";"
}

fun baz(x: String) = "($x)"

private inline fun foo(): String {
    return baz(bar { "OK" })
}

private inline fun bar(noinline x: () -> String): String {
    return "[" + baz(boo { shouldBeInlined(); x() }) + "]"
}

fun boo(x: () -> String) = x()

private inline fun shouldBeInlined() {
    log("shouldBeInlined")
}

fun box(): String {
    val result = foo()
    if (result != "([(OK)])") return "fail: $result"
    return "OK"
}