interface IntfWithoutDefaultImpls

interface IntfWithDefaultImpls {
    fun a() {}
}

interface Intf {
    companion object {
        val BLACK = 1
        const val WHITE = 2
    }

    val color: Int
        get() = BLACK
}