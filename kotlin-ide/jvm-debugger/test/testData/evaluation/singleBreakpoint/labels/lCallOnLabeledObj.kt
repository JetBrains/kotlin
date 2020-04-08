package lCallOnLabeledObj

fun main(args: Array<String>) {
    val myClass = MyClass()
    //Breakpoint!
    val a = 1
}

class MyClass {
    fun foo() = 1
}

// DEBUG_LABEL: myClass = myClass

// EXPRESSION: myClass_DebugLabel.foo()
// RESULT: 1: I