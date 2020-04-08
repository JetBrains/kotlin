package stepOutInlineFunctionStdlib

fun main(args: Array<String>) {
    val a = listOf(1, 2, 3)
    //Breakpoint!
    a.firstOrNull {
        it > 1
    }
}

fun test(i: Int) = 1

// TRACING_FILTERS_ENABLED: false
// STEP_INTO: 1
// STEP_OUT: 1
