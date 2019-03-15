// EXPECTED_REACHABLE_NODES: 1294

@JsName("AA") object A {
    @JsName("foo") fun bar() = "A.foo"
}

@JsName("BB") class B {
    @JsName("foo") fun bar() = "B.foo"
}

fun testA() = js("""
var a = JS_TESTS.AA;
return a.foo();
""")

fun testB() = js("""
var b = new JS_TESTS.BB();
return b.foo();
""")


fun box(): String {
    assertEquals("A.foo", testA())
    assertEquals("B.foo", testB())

    return "OK"
}