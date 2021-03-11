package test

internal open class ClassWithStatics {
    fun instanceMethod() {}

    companion object {

        fun staticMethod(p: Int) {}

        val staticField = 1
        var staticNonFinalField = 1

        var value = 0
    }
}