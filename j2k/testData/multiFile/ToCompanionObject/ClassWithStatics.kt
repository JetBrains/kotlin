package test

internal open class ClassWithStatics {
    fun instanceMethod() {
    }

    companion object {

        fun staticMethod(p: Int) {
        }

        val staticField: Int = 1

        var value: Int = 0
    }
}