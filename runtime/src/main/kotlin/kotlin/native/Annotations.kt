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

/**
 * Preserve the function entry point during global optimizations, only for the given target.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class RetainForTarget(val target: String)


/** @suppress */
@Deprecated("Use common kotlin.Throws annotation instead.", ReplaceWith("kotlin.Throws"), DeprecationLevel.WARNING)
public typealias Throws = kotlin.Throws

/** @suppress */
public typealias ThreadLocal = kotlin.native.concurrent.ThreadLocal

/** @suppress */
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

