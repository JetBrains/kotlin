// WITH_STDLIB

open class A {
    open fun openMethod(a: UInt): String = "A"

    @Anno2
    open fun openMethodAnnotated(a: UInt): String = "A"
}

@kotlin.annotation.Retention(kotlin.annotation.AnnotationRetention.RUNTIME)
annotation class Anno

@kotlin.annotation.Retention(kotlin.annotation.AnnotationRetention.RUNTIME)
annotation class Anno2

class B : A() {
    @JvmExposeBoxed
    override fun openMethod(a: UInt): String = "B"

    @JvmExposeBoxed
    fun ownMethod(a: UInt): String = "own"

    @JvmExposeBoxed
    @Anno
    override fun openMethodAnnotated(a: UInt): String = "B"

    @JvmExposeBoxed
    @Anno
    fun ownMethodAnnotated(a: UInt): String = "own"
}

class C {
    @JvmExposeBoxed
    override fun toString(): String = "C"
}
