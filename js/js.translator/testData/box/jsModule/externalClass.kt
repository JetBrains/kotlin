// MODULE_KIND: AMD
package foo

@JsModule("lib")
@native class A(@native val x: Int = noImpl) {
    @native fun foo(y: Int): Int = noImpl
}

fun box(): String {
    val a = A(23)
    assertEquals(23, a.x)
    assertEquals(65, a.foo(42))

    return "OK"
}