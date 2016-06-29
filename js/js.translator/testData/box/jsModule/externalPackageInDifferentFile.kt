// MODULE_KIND: AMD
// FILE: lib.kt
@file:JsModule("lib")
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

// FILE: lib2.kt
package foo

@native object C {
    fun f(): Int = noImpl
}

// FILE: main.kt
package foo

fun box(): String {
    val a = A(23)
    assertEquals(23, a.x)
    assertEquals(65, a.foo(42))

    assertEquals(123, B.x)
    assertEquals(265, B.foo(142))

    assertEquals(365, foo(42))
    assertEquals(423, bar)

    assertEquals(12345, C.f())

    mbar = 523
    assertEquals(523, mbar)

    return "OK"
}