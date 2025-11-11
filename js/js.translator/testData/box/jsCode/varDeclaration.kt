package foo

fun box(): String {
    js("""
        var a = 123;
        const b = 123;
        let c = 123;
        c = a + b
    """)

    assertEquals(123, js("a"))
    assertEquals(123, js("b"))
    assertEquals(246, js("c"))

    return "OK"
}
