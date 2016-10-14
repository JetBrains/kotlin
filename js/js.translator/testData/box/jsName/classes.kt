package foo

@JsName("AA") object A {
    @JsName("foo") fun bar() = "A.foo"
}

@JsName("BB") class B {
    @JsName("foo") fun bar() = "B.foo"
}

fun testA() = js("""
var a = Kotlin.modules.JS_TESTS.foo.AA;
return a.foo();
""")

fun testB() = js("""
var b = new Kotlin.modules.JS_TESTS.foo.BB();
return b.foo();
""")


fun box(): String {
    assertEquals("A.foo", testA())
    assertEquals("B.foo", testB())

    return "OK"
}