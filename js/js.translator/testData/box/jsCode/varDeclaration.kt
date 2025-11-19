// FILE: a.kt
inline fun withDeclared(action: () -> Unit) {
    js("""
        var a = 123;
        const b = 123;
        let c = 123;
        c = a + b
    """)
    action()
}

// FILE: b.kt
// RECOMPILE

fun box(): String {
    withDeclared {
        assertEquals(123, js("a"))
        assertEquals(123, js("b"))
        assertEquals(246, js("c"))
    }

    return "OK"
}
