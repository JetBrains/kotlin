package stepOutInlinedLambdaArgumentOneLine

fun main(args: Array<String>) {
    //Breakpoint! (lambdaOrdinal = 1)
    dive(3) { x -> x + 4 }
}

inline fun dive(p: Int, f:(Int) -> Int) = f(p)

// STEP_OUT: 2