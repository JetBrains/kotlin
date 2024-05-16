package base

inline fun <V> nullUnless(condition: Boolean, fn: () -> V): V? = if (condition) fn() else null
