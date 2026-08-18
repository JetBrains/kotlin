// WITH_STDLIB

open class P {
    open val x: UInt get() = 1u

    @get:Anno2
    open val x_annotated: UInt get() = 1u
}

@kotlin.annotation.Retention(kotlin.annotation.AnnotationRetention.RUNTIME)
annotation class Anno

@kotlin.annotation.Retention(kotlin.annotation.AnnotationRetention.RUNTIME)
annotation class Anno2

class Q : P() {
    @get:JvmExposeBoxed
    override val x: UInt get() = 2u

    @get:JvmExposeBoxed
    @all:Anno
    override val x_annotated: UInt get() = 2u
}

interface I {
    val y: Int
}

@JvmInline
value class VC(val z: Int) : I {
    override val y: Int get() = z
}
