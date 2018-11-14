// EXPECTED_REACHABLE_NODES: 1286
var global = ""

fun log(message: String) {
    global += message + ";"
}

fun baz(x: String) = "($x)"

inline fun foo(): String {
    return baz(bar { "OK" })
}

inline fun bar(noinline x: () -> String): String {
    return "[" + baz(boo { shouldBeInlined(); x() }) + "]"
}

fun boo(x: () -> String) = x()

inline fun shouldBeInlined() {
    log("shouldBeInlined")
}

fun box(): String {
    val result = foo()
    if (result != "([(OK)])") return "fail1: $result"
    if (global != "shouldBeInlined;") return "fail2: $global"
    return "OK"
}