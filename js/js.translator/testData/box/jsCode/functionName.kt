fun box(): String {
    return js("""
        function foo() {
            return "OK";
        }
        foo();
    """)
}