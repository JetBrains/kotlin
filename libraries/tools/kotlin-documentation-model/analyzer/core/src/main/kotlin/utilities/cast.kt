package org.jetbrains.dokka.utilities

inline fun <reified T> Any.cast(): T {
    return this as T
}
