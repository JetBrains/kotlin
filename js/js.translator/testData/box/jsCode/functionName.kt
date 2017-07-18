// EXPECTED_REACHABLE_NODES: 990
fun box(): String {
    return js("""
        function foo() {
            return "OK";
        }
        foo();
    """)
}