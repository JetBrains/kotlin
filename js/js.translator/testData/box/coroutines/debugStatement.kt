// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1292

fun foo(f: suspend () -> Unit) {
}

fun box(): String {
    foo {
        println("aaa")
        js("debugger;")
        println("bbb")
    }
    return "OK"
}