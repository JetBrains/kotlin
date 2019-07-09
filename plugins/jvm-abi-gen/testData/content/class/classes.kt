package test

interface Interface {
}

open class BaseClass {
    open val baseClassPublicVal: Int = 0
    open fun baseClassPublicFun(): Int = 1

    internal val baseClassInternalVal: Int = 2
    internal fun baseClassInternalFun(): Int = 3

    protected val baseClassProtectedVal: Int = 4
    protected fun baseClassProtectedFun(): Int = 5

    private val baseClassPrivateVal: Int = 6
    private fun baseClassPrivateFun(): Int = 7

    companion object {
        const val basePublicConst: Int = 8
        private const val basePrivateConst: Int = 9
    }
}

class Class : BaseClass(), Interface {
    fun classPublicMethod() {
        class publicMethodLocalClass {
            val x = 0
        }

        val publicMethodLambda: (Int) -> Int = { it * it }
    }

    private class NestedInnerClass() {
        class NestedNestedInnerClass() {}
    }
}

private class PrivateClass