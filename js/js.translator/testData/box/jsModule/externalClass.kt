// MODULE_KIND: AMD
package foo

@JsModule("lib")
external class A(val x: Int = noImpl) {
    fun foo(y: Int): Int = noImpl
}

fun box(): String {
    val a = A(23)
    assertEquals(23, a.x)
    assertEquals(65, a.foo(42))

    return "OK"
}