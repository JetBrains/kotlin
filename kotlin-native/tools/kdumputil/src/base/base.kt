package base

inline fun <V> V.runIf(condition: Boolean, fn: V.() -> V): V = if (condition) fn() else this

inline fun <V> nullUnless(condition: Boolean, fn: () -> V): V? = if (condition) fn() else null
