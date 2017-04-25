// MINIFICATION_THRESHOLD: 535
fun box(): String {
    return js("""
        function foo() {
            return "OK";
        }
        foo();
    """)
}