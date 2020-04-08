package test

internal open class ClassWithStatics {
    fun instanceMethod() {}

    companion object {
        @JvmStatic
        fun staticMethod(p: Int) {}
        const val staticField = 1
        @JvmField
        var staticNonFinalField = 1
        var value = 0
    }
}