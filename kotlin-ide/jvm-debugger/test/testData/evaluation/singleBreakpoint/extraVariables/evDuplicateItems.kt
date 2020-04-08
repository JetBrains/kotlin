package evDuplicateItems

class A {
    var prop = 1
}

fun main(args: Array<String>) {
    val a = A()
    //Breakpoint!
    val b = a.prop + a.prop
}

// PRINT_FRAME