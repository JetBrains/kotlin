package internalProperty

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

class MyClass {
    internal val internalVal = 42
    internal val internalValWithGetter: Int get() = 24
}

// EXPRESSION: MyClass().internalVal
// RESULT: 42: I

// EXPRESSION: MyClass().internalValWithGetter
// RESULT: 24: I