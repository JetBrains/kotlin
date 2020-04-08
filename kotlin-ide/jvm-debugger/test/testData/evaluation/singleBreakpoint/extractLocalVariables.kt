package extractLocalVariables

fun main(args: Array<String>) {
    val a = 1
    val klass = MyClass()
    //Breakpoint!
    klass.f1(a)
}

class MyClass {
    val b = 1
    fun f1(p1: Int) = p1
}

// EXPRESSION: a
// RESULT: 1: I

// EXPRESSION: klass.f1(1)
// RESULT: 1: I

// EXPRESSION: args.size
// RESULT: 0: I

// EXPRESSION: klass.b
// RESULT: 1: I
