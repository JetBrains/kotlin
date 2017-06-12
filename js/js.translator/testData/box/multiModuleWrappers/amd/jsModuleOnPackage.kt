// EXPECTED_REACHABLE_NODES: 500
// MODULE: lib
// FILE: lib.kt
// MODULE_KIND: AMD
@file:JsModule("native-lib")
package foo

external class A(x: Int = definedExternally) {
    val x: Int

    fun foo(y: Int): Int = definedExternally
}

external object B {
    val x: Int = definedExternally

    fun foo(y: Int): Int = definedExternally
}

external fun foo(y: Int): Int = definedExternally

external val bar: Int = definedExternally

external var mbar: Int = definedExternally

// MODULE: main(lib)
// FILE: main.kt
// MODULE_KIND: AMD
package foo

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