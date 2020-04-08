open class B {
    open val v: Int = 0
}

interface I {
    val v: Int
}

class A : B(), I {
    override val v: Int
        get() = super<caret><B>.v
}