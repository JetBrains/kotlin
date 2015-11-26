@file:kotlin.jvm.JvmName("TimingUtilsKt")
package kotlin.util

/**
 * Executes the given block and returns elapsed time in milliseconds.
 */
@Deprecated("Use measureTimeMillis from system.timing instead.", ReplaceWith("kotlin.system.measureTimeMillis(block)", "kotlin.system"))
public fun measureTimeMillis(block: () -> Unit) : Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}

/**
 * Executes the given block and returns elapsed time in nanoseconds.
 */
@Deprecated("Use measureTimeNano from system.timing instead.", ReplaceWith("kotlin.system.measureTimeNano(block)", "kotlin.system"))
public fun measureTimeNano(block: () -> Unit) : Long {
    val start = System.nanoTime()
    block()
    return System.nanoTime() - start
}
