package stepIntoStdlibFacadeClass

fun main(args: Array<String>) {
    val a = intArrayOf(1)
    //Breakpoint!
    a.withIndex()
    val b = 1
}

// STEP_INTO: 2
// TRACING_FILTERS_ENABLED: false
// SKIP_SYNTHETIC_METHODS: false