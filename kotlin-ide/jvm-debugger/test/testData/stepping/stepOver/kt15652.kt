package test

fun main() {
    runBlock {
        //Breakpoint!
        throw Exception()
    }
}

inline fun runBlock(block: () -> Unit) {
    block()
}

// STEP_OVER: 4