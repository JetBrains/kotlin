@file:kotlin.jvm.JvmName("TimingUtilsKt")
package kotlin.util

/**
 * Executes the given block and returns elapsed time in milliseconds.
 */
public fun measureTimeMillis(block: () -> Unit) : Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}

/**
 * Executes the given block and returns elapsed time in nanoseconds.
 */
public fun measureTimeNano(block: () -> Unit) : Long {
    val start = System.nanoTime()
    block()
    return System.nanoTime() - start
}
