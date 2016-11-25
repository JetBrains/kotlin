// MODULE_KIND: AMD
package foo

@JsModule("lib")
external object A {
    val x: Int = noImpl

    fun foo(y: Int): Int = noImpl
}

fun box(): String {
    assertEquals(23, A.x)
    assertEquals(65, A.foo(42))
    return "OK"
}