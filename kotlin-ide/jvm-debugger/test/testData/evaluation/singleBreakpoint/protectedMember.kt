package protectedMember

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

class MyClass {
    protected fun protectedFun(): Int = 1
    protected val protectedVal: Int = 1

    protected class ProtectedClass {
        val a = 1
    }
}

// EXPRESSION: MyClass().protectedFun()
// RESULT: 1: I

// EXPRESSION: MyClass().protectedVal
// RESULT: 1: I

// EXPRESSION: MyClass.ProtectedClass().a
// RESULT: 1: I