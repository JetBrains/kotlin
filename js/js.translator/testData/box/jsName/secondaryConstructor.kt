package foo

class A(val x: String) {
    @JsName("A_int") constructor(x: Int) : this("int $x")
}

fun test() = js("""
return Kotlin.modules.JS_TESTS.foo.A_int(23).x;
""")

fun box(): String {
    val result = test()
    assertEquals("int 23", result);
    return "OK"
}