package test

class A {
    val publicVal = 0
    fun publicMethod() = 0

    internal val internalVal = 0
    internal fun internalMethod() = 0

    protected val protectedVal = 0
    protected fun protectedMethod() = 0

    val publicValBody = internalMethod() + 1
    val publicMethodBody = publicValBody + 1
}