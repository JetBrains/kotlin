// JDK_KIND: FULL_JDK_21
// NO_VALIDATION
annotation class A

interface I {
    fun foo(): Int
}

@A
@JvmRecord
data class R<T>(@A val x: Int, val y: T): I {
    constructor(y: T) : this(0, y)

    override fun foo(): Int  = 0
}