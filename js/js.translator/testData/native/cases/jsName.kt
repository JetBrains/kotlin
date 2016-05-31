package foo

@JsName("bar") @native fun foo(): Int = noImpl

@JsName("baz") @native val prop: Int get() = noImpl

@JsName("B") @native class A {
    @JsName("g") fun f(): Int = noImpl

    @JsName("q") val p: Int get() = noImpl

    companion object {
        @JsName("g") fun f(): Int = noImpl

        @JsName("q") val p: Int get() = noImpl
    }
}

@JsName("P") @native object O {
    fun f(): Int = noImpl
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