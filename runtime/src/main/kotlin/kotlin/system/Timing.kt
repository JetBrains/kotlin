package kotlin.system

@PublishedApi
@SymbolName("Kotlin_system_getTimeMillis")
internal external fun getTimeMillis() : Long

@PublishedApi
@SymbolName("Kotlin_system_getTimeNanos")
internal external fun getTimeNanos() : Long

@PublishedApi
@SymbolName("Kotlin_system_getTimeMicros")
internal external fun getTimeMicros() : Long

/** Executes the given block and returns elapsed time in milliseconds. */
public inline fun measureTimeMillis(block: () -> Unit) : Long {
    val start = getTimeMillis()
    block()
    return getTimeMillis() - start
}

/** Executes the given block and returns elapsed time in microseconds (Kotlin/Native only). */
public inline fun measureTimeMicros(block: () -> Unit) : Long {
    val start = getTimeMicros()
    block()
    return getTimeMicros() - start
}

/** Executes the given block and returns elapsed time in nanoseconds. */
public inline fun measureNanoTime(block: () -> Unit) : Long {
    val start = getTimeNanos()
    block()
    return getTimeNanos() - start
}
