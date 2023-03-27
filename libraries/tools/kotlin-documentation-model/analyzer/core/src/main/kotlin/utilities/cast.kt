package org.jetbrains.dokka.utilities

import org.jetbrains.dokka.*

@InternalDokkaApi
inline fun <reified T> Any.cast(): T {
    return this as T
}
