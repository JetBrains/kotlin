package org.jetbrains.dokka.utilities

import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
inline fun <reified T> Any.cast(): T {
    return this as T
}
