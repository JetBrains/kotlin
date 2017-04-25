// EXPECTED_REACHABLE_NODES: 497
package foo

@JsName("bar")
external fun foo(): Int = definedExternally

@JsName("baz")
external val prop: Int get() = definedExternally

@JsName("B")
external class A {
    @JsName("g")
    fun f(): Int = definedExternally

    @JsName("q")
    val p: Int get() = definedExternally

    companion object {
        @JsName("g")
        fun f(): Int = definedExternally

        @JsName("q")
        val p: Int get() = definedExternally
    }
}

@JsName("P")
external object O {
    fun f(): Int = definedExternally
}

fun box(): String {
    assertEquals(23, foo())
    assertEquals(123, prop)

    assertEquals(42, A().f())
    assertEquals(32, A().p)

    assertEquals(142, A.f())
    assertEquals(132, A.p)

    assertEquals(222, O.f())

    return "OK"
}