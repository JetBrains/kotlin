package foo

@native class A(x: Int) {
    var x: Int
        get() = noImpl
        set(value) = noImpl

    fun foo(): Int = noImpl

    class B(val value: Int) {
        fun bar(): Int = noImpl
    }

    inner class C(val value: Int) {
        fun bar(): Int  = noImpl
        fun dec(): Unit = noImpl
    }
}

fun box(): String {
    var b = A.B(23)
    if (b.bar() != 10023) return "failed1: ${b.bar()}"

    var c = A(11).C(23)
    if (c.bar() != 10034) return "failed2: ${c.bar()}"
    c.dec()
    if (c.bar() != 10033) return "failed3: ${c.bar()}"

    return "OK"
}

