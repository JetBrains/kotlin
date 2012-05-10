package kotlin.util

/**
Executes current block and returns elapsed time in milliseconds
*/
public fun measureTimeMillis(block: () -> Unit) : Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}

/**
Executes current block and returns elapsed time in nanoseconds
*/
public fun measureTimeNano(block: () -> Unit) : Long {
    val start = System.nanoTime()
    block()
    return System.nanoTime() - start
}
