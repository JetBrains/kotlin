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

// LINES(JS):    1 2 3 2 2 2 2 2 2 2 * 11 12 12 12 15 15 15 16 18 17 17
// LINES(JS_IR): 11 11 * 11 11 * 1 1 3 3 1 1 1 1 1 1 1 1 1 * 11 11 12 12 12 16 17 17 15 15 15 * 1 * 11
