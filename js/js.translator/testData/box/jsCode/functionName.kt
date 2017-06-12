// EXPECTED_REACHABLE_NODES: 487
fun box(): String {
    return js("""
        function foo() {
            return "OK";
        }
        foo();
    """)
}