// MODULE_KIND: AMD
package foo

@JsModule("lib")
@native object A {
    @native val x: Int = noImpl

    @native fun foo(y: Int): Int = noImpl
}

fun box(): String {
    assertEquals(23, A.x)
    assertEquals(65, A.foo(42))
    return "OK"
}