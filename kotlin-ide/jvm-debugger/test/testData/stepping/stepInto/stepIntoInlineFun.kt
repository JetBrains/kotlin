package stepIntoInlineFun

class A()

fun main(args: Array<String>) {
    val a = A()
    //Breakpoint!
    a.test { it + 1 }
    val b = 1
}

inline fun A.test(l: (Int) -> Unit) {
    l(11)
}

// STEP_INTO: 2
// TRACING_FILTERS_ENABLED: false
