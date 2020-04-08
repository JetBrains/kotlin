package functionCallStoredToVariable

fun main(args: Array<String>) {
    val a = inlineFunInt {
        // STEP_OVER: 1
        //Breakpoint!
        1
    }

    // RESUME: 1
    val b = simpleFunInt {
        // STEP_OVER: 2
        //Breakpoint!
        1
    }

    // RESUME: 1
    val c = inlineFunVoid {
        // STEP_OVER: 2
        //Breakpoint!
        val aa = 1
    }

    // RESUME: 1
    val d = simpleFunVoid {
        // STEP_OVER: 3
        //Breakpoint!
        val aa = 1
    }
}

inline fun inlineFunInt(f: () -> Int): Int {
    val a = 1
    return f()
}

inline fun inlineFunVoid(f: () -> Unit): Unit {
    val a = 1
    return f()
}

fun simpleFunInt(f: () -> Int): Int {
    return f()
}

fun simpleFunVoid(f: () -> Unit): Unit {
    return f()
}