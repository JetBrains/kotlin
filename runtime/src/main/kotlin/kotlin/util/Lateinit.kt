@file:Suppress("unused", "WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE")

package kotlin

import kotlin.internal.InlineOnly
import kotlin.internal.AccessibleLateinitPropertyLiteral
import kotlin.reflect.KProperty0
import konan.internal.Intrinsic

/**
 * Returns `true` if this lateinit property has been assigned a value, and `false` otherwise.
 *
 * Cannot be used in an inline function, to avoid binary compatibility issues.
 */
@SinceKotlin("1.2")
@InlineOnly
@Intrinsic
inline val @receiver:AccessibleLateinitPropertyLiteral KProperty0<*>.isInitialized: Boolean
    get() = throw NotImplementedError("Implementation is intrinsic")
