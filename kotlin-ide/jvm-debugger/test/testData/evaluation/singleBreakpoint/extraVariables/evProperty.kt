package evProperty

class A {
    var prop = 1
}

fun main(args: Array<String>) {
    val a = A()
    //Breakpoint!
    if (a.prop == 1) {

    }
}

// PRINT_FRAME