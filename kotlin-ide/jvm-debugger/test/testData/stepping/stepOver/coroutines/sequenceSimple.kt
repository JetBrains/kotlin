package test

fun main() {
    val seq = sequence {
        //Breakpoint!
        yield(1)
        yield(2)
        yield(3)
    }

    seq.toList()
}

// STEP_OVER: 4