// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1280
fun box(): String {
    return js("""
        function foo() {
            return "OK";
        }
        foo();
    """)
}