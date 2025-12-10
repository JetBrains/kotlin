package cases.private

private open class PrivateClass public constructor() {
    internal val internalVal = 1

    protected fun protectedFun() = internalVal
}
