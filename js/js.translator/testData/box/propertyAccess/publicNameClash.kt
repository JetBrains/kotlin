// EXPECTED_REACHABLE_NODES: 1284

// KT-33327

class C {
    var foo: String = "FAIL1"

    fun foo(foo: String): C {
        this.foo = foo
        return this
    }

    @JsName("_bar")
    var bar: String = "FAIL2"

    fun bar(bar: String): C {
        this.bar = bar
        return this
    }
}

fun box(): String {
    val c = C()
    c.foo("O").bar("K")

    return c.foo + c.bar
}