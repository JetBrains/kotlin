/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

/**
 * This annotation indicates what exceptions should be declared by a function when compiled to a platform method.
 *
 * When compiling to Objective-C/Swift framework,  non-`suspend`  functions having or inheriting
 * this annotation are represented as `NSError*`-producing methods in Objective-C
 * and as `throws` methods in Swift. Representations for `suspend` functions always have
 * `NSError*`/`Error` parameter in completion handler
 *
 * When Kotlin function called from Swift/Objective-C code throws an exception
 * which is an instance of one of the [exceptionClasses] or their subclasses,
 * it is propagated as `NSError`. Other Kotlin exceptions reaching Swift/Objective-C
 * are considered unhandled and cause program termination.
 *
 * Note: `suspend` functions without `@Throws` propagate only
 * [kotlin.coroutines.cancellation.CancellationException] as `NSError`.
 * Non-`suspend` functions without `@Throws` don't propagate Kotlin exceptions at all.
 *
 * @property exceptionClasses the list of checked exception classes that may be thrown by the function.
 */
@SinceKotlin("1.4")
@Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT")
@Target(FUNCTION, CONSTRUCTOR)
@Retention(BINARY)
public actual annotation class Throws(actual vararg val exceptionClasses: KClass<out Throwable>)
