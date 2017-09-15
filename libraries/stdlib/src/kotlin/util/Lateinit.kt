@file:kotlin.jvm.JvmName("LateinitKt")
@file:kotlin.jvm.JvmVersion
@file:Suppress("unused")

package kotlin

import kotlin.internal.InlineOnly
import kotlin.internal.AccessibleLateinitPropertyLiteral
import kotlin.reflect.KProperty0

/**
 * Returns `true` if this lateinit property has been assigned a value, and `false` otherwise.
 *
 * Cannot be used in an inline function, to avoid binary compatibility issues.
 */
@SinceKotlin("1.2")
@InlineOnly
inline val @receiver:AccessibleLateinitPropertyLiteral KProperty0<*>.isInitialized: Boolean
    get() = throw NotImplementedError("Implementation is intrinsic")
