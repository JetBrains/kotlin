/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.native

import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

/**
 * Marks the declaration as error-producing when exported to Swift or Objective-C.
 *
 * Methods having or inheriting this annotation are represented as
 * `NSError*`-producing methods in Objective-C and as `throws` methods in Swift.
 * When such a method is called from Swift or Objective-C and throws an exception,
 * the exception is either propagated as `NSError` or considered unhandled
 * (if it `is` [kotlin.Error] or [kotlin.RuntimeException]).
 * In any case the exception is not checked to be instance of one of the [exceptionClasses].
 *
 * @property exceptionClasses the list of checked exception classes that may be thrown by the function.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@OptionalExpectation
public expect annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)