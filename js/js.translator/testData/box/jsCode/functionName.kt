// EXPECTED_REACHABLE_NODES: 1374
fun box(): String {
    return js("""
        function foo() {
            return "OK";
        }
        foo();
    """)
}