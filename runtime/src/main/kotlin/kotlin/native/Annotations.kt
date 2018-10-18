/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native

import kotlin.reflect.KClass

/**
 * Forces the compiler to use specified symbol name for the target `external` function.
 *
 * TODO: changing symbol name breaks the binary compatibility,
 * so it should probably be allowed on `internal` and `private` functions only.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class SymbolName(val name: String)

/**
 * Preserve the function entry point during global optimizations.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Retain

// TODO: merge with [kotlin.jvm.Throws]
/**
 * This annotation indicates what exceptions should be declared by a function when compiled to a platform method.
 *
 * When compiling to Objective-C/Swift framework, methods having or inheriting this annotation are represented as
 * `NSError*`-producing methods in Objective-C and as `throws` methods in Swift.
 * When such a method called through framework API throws an exception, it is either propagated as
 * `NSError` or considered unhandled (if exception `is` [kotlin.Error] or [kotlin.RuntimeException]).
 * In any case exception is not checked to be instance of one of the [exceptionClasses].
 *
 * @property exceptionClasses the list of checked exception classes that may be thrown by the function.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
public annotation class Throws(vararg val exceptionClasses: KClass<out Throwable>)

@Deprecated("ThreadLocal was moved to kotlin.native.concurrent package " +
        "and became available in Kotlin Common as optional annotation",
        ReplaceWith("kotlin.native.concurrent.ThreadLocal"))
public typealias ThreadLocal = kotlin.native.concurrent.ThreadLocal

@Deprecated("SharedImmutable was moved to kotlin.native.concurrent package " +
        "and became available in Kotlin Common as optional annotation",
        ReplaceWith("kotlin.native.concurrent.SharedImmutable"))
public typealias SharedImmutable = kotlin.native.concurrent.SharedImmutable

/**
 * Makes top level function available from C/C++ code with the given name.
 *
 * [externName] controls the name of top level function, [shortName] controls the short name.
 * If [externName] is empty, no top level declaration is being created.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class CName(val externName: String = "", val shortName: String = "")