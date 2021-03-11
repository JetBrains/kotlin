package testing.rename

interface A {
    fun first() : Int
}

public open class B: A {
    override fun first() = 1
}

class C: B() {
    override fun first() = 2
}

fun usages() {
    val b = B()
    val a: A = b
    val c = C()

    a.first()
    b.first()
    c.first()
}


