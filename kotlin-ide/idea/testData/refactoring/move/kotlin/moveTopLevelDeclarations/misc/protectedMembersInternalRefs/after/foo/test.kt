package foo

open class Older {
    protected object ProtectedObject { val inProtectedObject = 0 }
    protected class ProtectedClass { val inProtectedClass = 0 }
    protected fun protectedFun() = 0
    protected var protectedVar = 0
}

