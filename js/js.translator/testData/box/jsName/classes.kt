// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1294

@JsName("AA") object A {
    @JsName("foo") fun bar() = "A.foo"
}

@JsName("BB") class B {
    @JsName("foo") fun bar() = "B.foo"
}

fun testA() = js("""
var a = main.AA;
return a.foo();
""")

fun testB() = js("""
var b = new main.BB();
return b.foo();
""")


fun box(): String {
    assertEquals("A.foo", testA())
    assertEquals("B.foo", testB())

    return "OK"
}