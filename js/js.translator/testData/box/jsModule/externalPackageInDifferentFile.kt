// MODULE_KIND: AMD
// FILE: lib.kt
@file:JsModule("lib")
package foo

external class A(@native val x: Int = noImpl) {
    fun foo(y: Int): Int = noImpl
}

external object B {
    val x: Int = noImpl

    fun foo(y: Int): Int = noImpl
}

external fun foo(y: Int): Int = noImpl

external val bar: Int = noImpl

external var mbar: Int = noImpl

// FILE: lib2.kt
package foo

external object C {
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