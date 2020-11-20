// EXPECTED_REACHABLE_NODES: 1244
package foo

external fun Function(vararg argsAndCode: String): Function

external interface Function {
    @nativeInvoke
    operator fun invoke(a: Any?): Any?

    @nativeInvoke
    fun baz(a: Any?, b: Any? = definedExternally, c: Any? = definedExternally): Any?
}

@nativeInvoke
operator fun Function.invoke(a: Any?, b: Any?): Any? = definedExternally

@nativeInvoke
fun Function.bar(a: Any?, b: Any? = definedExternally, c: Any? = definedExternally): Any? = definedExternally

object t{}

fun box(): String {
    val f = Function("a", "return a")
    val g = Function("a", "b", "c", "return a + (b || 10) + (c || 100)")

    assertEquals(1, f(1))
    assertEquals("ok", f("ok"))
    assertEquals(t, f(t))

    assertEquals(105, g(1, 4))
    assertEquals("ok34100", g("ok", 34))

    assertEquals(105, g.baz(1, 4))
    assertEquals("ok34100", g.baz("ok", 34))
    assertEquals("ok1034", g.baz("ok", c = 34))

    assertEquals(105, g.bar(1, 4))
    assertEquals("ok34100", g.bar("ok", 34))
    assertEquals("ok1034", g.bar("ok", c = 34))

    return "OK"
}
