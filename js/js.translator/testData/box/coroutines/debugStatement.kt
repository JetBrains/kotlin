// EXPECTED_REACHABLE_NODES: 1292
// CHECK_DEBUGGER_COUNT: function=doResume count=1

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