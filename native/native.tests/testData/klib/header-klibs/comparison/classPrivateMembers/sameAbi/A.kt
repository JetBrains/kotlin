package test

class A {
    val publicVal = 0
    fun publicMethod() = 0

    internal val internalVal = 42
    internal fun internalMethod() = 42

    protected val protectedVal = 0
    protected fun protectedMethod() = 0

    val publicValBody = internalMethod() + 1
    val publicMethodBody = publicValBody + 1
}