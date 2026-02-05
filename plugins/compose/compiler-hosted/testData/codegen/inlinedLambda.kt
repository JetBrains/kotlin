// DUMP_IR

// MODULE: a
// MODULE_KIND: LibraryBinary
// FILE: com/example/home/a.kt
package com.example.home

@JvmInline
value class Outcome<out I, out T> private constructor(private val value: Any) {
    val isIncomplete: Boolean get() = value is Incomplete

    fun incompleteOrNull(): I? = (value as? Incomplete)?.incompleteValue as? I
    fun getOrNull(): T? = if (value is Incomplete) null else value as? T

    inline fun <R> fold(
        onComplete: (value: T) -> R,
        onIncomplete: (incomplete: I) -> R,
    ): R {
        return when (isIncomplete) {
            true -> onIncomplete(incompleteOrNull() as I)
            false -> onComplete(getOrNull() as T)
        }
    }

    private data class Incomplete(val incompleteValue: Any?)
}

// MODULE: main(a)
// FILE:  com/example/home/main.kt
package com.example.home

internal inline fun <I, T : R, R> Outcome<I, T>.getOrElse(onIncomplete: (incomplete: I) -> R): R {
    return fold(
        onComplete = { it },
        onIncomplete = onIncomplete,
    )
}