package cases.protected

public open class PublicOpenClass protected constructor() {
    protected val protectedVal = 1
    protected var protectedVar = 2

    protected fun protectedFun() = protectedVal
}
