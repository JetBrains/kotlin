/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

// Note:
// Right now we don't want to have neither 'volatile' nor 'synchronized' at runtime, as it has different
// concurrency approach.

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
@Deprecated("Do not use Volatile annotation in pure Kotlin/Native code", level = DeprecationLevel.ERROR)
public annotation class Volatile

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.SOURCE)
@Deprecated("Do not use Synchronized annotation in pure Kotlin/Native code", level = DeprecationLevel.ERROR)
public annotation class Synchronized

/**
 * An actual implementation of `synchronized` method. This method is not supported in Kotlin/Native
 * @throws UnsupportedOperationException always
 */
@kotlin.internal.InlineOnly
@Deprecated("Do not use 'synchronized' function in Kotlin/Native code", level = DeprecationLevel.ERROR)
public actual inline fun <R> synchronized(@Suppress("UNUSED_PARAMETER") lock: Any, block: () -> R): R =
        throw UnsupportedOperationException("synchronized() is unsupported")