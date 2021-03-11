package foo

import bar.Older

class Younger : Older() {
    protected val v1: ProtectedObject = ProtectedObject
    val v2 = ProtectedObject.inProtectedObject
    protected val v3: ProtectedClass = ProtectedClass()
    val v4 = ProtectedClass().inProtectedClass
    val v5 = protectedFun()
    val v6 = protectedVar
}