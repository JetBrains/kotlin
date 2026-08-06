// WITH_STDLIB

open class P {
    open val x: UInt get() = 1u
}

class Q : P() {
    @get:JvmExposeBoxed
    override val x: UInt get() = 2u
}

interface I {
    val y: Int
}

@JvmInline
value class VC(val z: Int) : I {
    override val y: Int get() = z
}
