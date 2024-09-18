// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_ES6

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