// EXPECTED_REACHABLE_NODES: 1249
fun box(): String {
    return js("""
        function foo() {
            return "OK";
        }
        foo();
    """)
}