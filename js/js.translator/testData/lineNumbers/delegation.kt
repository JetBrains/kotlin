class A :
    I
    by o

interface I {
    fun foo(): String

    var bar: Int
}

val o = object : I {
    override fun foo(): String = "foo"

    override var bar: Int
        get() = 23
        set(value) {
            println(value)
        }
}

// LINES(JS_IR): 11 11 * 11 11 * 1 1 3 3 1 1 1 1 1 1 1 1 1 * 11 11 12 12 12 16 17 17 15 15 15 * 1 * 11
