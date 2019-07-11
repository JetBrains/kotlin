// EXPECTED_REACHABLE_NODES: 1280
fun box(): String {
    return js("""
        function foo() {
            return "OK";
        }
        foo();
    """)
}