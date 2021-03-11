package frameExtensionFun

fun main(args: Array<String>) {
    A().foo()
}

class A {
    val prop = 1
    fun myFun() = 1
}

fun A.foo() {
    //Breakpoint!
    prop
}

// PRINT_FRAME

// EXPRESSION: prop
// RESULT: 1: I

// EXPRESSION: myFun()
// RESULT: 1: I