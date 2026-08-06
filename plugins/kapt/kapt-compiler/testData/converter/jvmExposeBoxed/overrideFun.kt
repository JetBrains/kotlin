// WITH_STDLIB

open class A {
    open fun openMethod(a: UInt): String = "A"
}

class B : A() {
    @JvmExposeBoxed
    override fun openMethod(a: UInt): String = "B"

    @JvmExposeBoxed
    fun ownMethod(a: UInt): String = "own"
}

class C {
    @JvmExposeBoxed
    override fun toString(): String = "C"
}
