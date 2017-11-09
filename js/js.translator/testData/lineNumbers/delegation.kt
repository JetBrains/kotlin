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

// LINES: 1 2 3 2 2 2 2 2 2 2 * 11 12 12 12 15 15 15 18 17 17