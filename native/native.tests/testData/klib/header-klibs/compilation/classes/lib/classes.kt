package lib

interface I {
    val iProperty: Int
    fun iMethod(): Int
}

open class A : I {
    override val iProperty: Int = 0
    override fun iMethod(): Int = 10

    val aProperty: Int = 20
    fun aMethod(): Int = 30
    inline fun aInlineMethod(): Int = 40

    private class AB {}

    companion object {
        const val aConst: Int = 50
    }
}

class B : A() {
    val bProperty: Int = 60
    fun bMethod(): Int = 70
    inline fun bInlineMethod(): Int = 80

    companion object {
        const val bConst: Int = 90
    }
}

value class C(private val b: String)