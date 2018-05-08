// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1108
fun box(): String {
    return js("""
        function foo() {
            return "OK";
        }
        foo();
    """)
}