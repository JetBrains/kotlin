fun foo() {
    <caret>val a = 1
}

class MyClass {
    protected fun protectedFun(): Int = 1
    protected val protectedVal: Int = 1

    protected class ProtectedClass {
        val a = 1
    }
}