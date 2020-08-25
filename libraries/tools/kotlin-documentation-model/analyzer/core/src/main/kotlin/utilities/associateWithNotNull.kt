package org.jetbrains.dokka.utilities

inline fun <K, V : Any> Iterable<K>.associateWithNotNull(valueSelector: (K) -> V?): Map<K, V> {
    @Suppress("UNCHECKED_CAST")
    return associateWith { valueSelector(it) }.filterValues { it != null } as Map<K, V>
}
