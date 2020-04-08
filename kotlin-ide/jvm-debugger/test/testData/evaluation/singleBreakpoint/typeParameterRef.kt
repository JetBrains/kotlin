package typeParameterRef

fun main(args: Array<String>) {
    test<Int>()
}

fun <U> foo(): Int {
    return 1
}

fun <T> test() {
    //Breakpoint!
    val a = foo<T>()
}

// EXPRESSION: foo<T>()
// RESULT: 1: I