package stepIntoStdLibInlineFun

fun main(args: Array<String>) {
    val a = listOf(1)
    //Breakpoint!
    a.map { it + 1 }
    val b = 1
}

// STEP_INTO: 1
// TRACING_FILTERS_ENABLED: false
