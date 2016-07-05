// MODULE_KIND: UMD
// NO_JS_MODULE_SYSTEM
@file:JsModule("lib")
@file:JsNonModule
package foo

@native class A(@native val x: Int = noImpl) {
    @native fun foo(y: Int): Int = noImpl
}

@native object B {
    @native val x: Int = noImpl

    @native fun foo(y: Int): Int = noImpl
}

@native fun foo(y: Int): Int = noImpl

@native val bar: Int = noImpl

@native var mbar: Int = noImpl

fun box(): String {
    val a = A(23)
    assertEquals(23, a.x)
    assertEquals(65, a.foo(42))

    assertEquals(123, B.x)
    assertEquals(265, B.foo(142))

    assertEquals(365, foo(42))
    assertEquals(423, bar)

    mbar = 523
    assertEquals(523, mbar)

    return "OK"
}