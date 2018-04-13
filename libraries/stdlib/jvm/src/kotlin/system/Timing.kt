@file:kotlin.jvm.JvmName("TimingKt")
@file:kotlin.jvm.JvmVersion
package kotlin.system

/**
 * Executes the given block and returns elapsed time in milliseconds.
 */
public inline fun measureTimeMillis(block: () -> Unit) : Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}

/**
 * Executes the given block and returns elapsed time in nanoseconds.
 */
public inline fun measureNanoTime(block: () -> Unit) : Long {
    val start = System.nanoTime()
    block()
    return System.nanoTime() - start
}
