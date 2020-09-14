/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

/**
 * Makes this function to be possible to call by given name from C++ part of runtime using C ABI.
 * The parameters are mapped in an implementation-dependent manner.
 *
 * The function to call from C++ can be a wrapper around the original function.
 *
 * If the name is not specified, the function to call will be available by its Kotlin unqualified name.
 *
 * This annotation is not intended for the general consumption and is public only for the launcher!
 */
@Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.BINARY)
public annotation class ExportForCppRuntime(val name: String = "")

/**
 * This annotation denotes that the element is intrinsic and its usages require special handling in compiler.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class Intrinsic

/**
 * Exports symbol for compiler needs.
 *
 * This annotation is not intended for the general consumption and is public only for interop!
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class ExportForCompiler

/**
 * Class is frozen by default. Also this annotation is (ab)used for marking objects
 * where mutability checks are not needed, and they are shared, such as atomics.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class Frozen

/**
 * Fields of annotated class won't be sorted.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class NoReorderFields

/**
 * Exports the TypeInfo of this class by given name to use it from runtime.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class ExportTypeInfo(val name: String)

/**
 * If a lambda shall be carefully lowered by the compiler.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class VolatileLambda

/**
 * Need to be fixed because of reflection.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
internal annotation class FixmeReflection

/**
 * Need to be fixed because of concurrency.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
internal annotation class FixmeConcurrency

/**
 * Escape analysis annotations.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class Escapes(val who: Int)

// Decyphering of binary values can be found in EscapeAnalysis.kt
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class PointsTo(vararg val onWhom: Int)


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class TypedIntrinsic(val kind: String)

/**
 * Indicates that `@SymbolName external` function is implemented in library-stored bitcode
 * and doesn't have native dependencies.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Independent

/**
 * Indicates that `@SymbolName external` function can throw foreign exception to be filtered on callsite.
 *
 * Note: this annotation describes rather behaviour of the (direct) call than that of the function.
 * E.g. it doesn't have any effect when calling the function virtually. TODO: rework.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@PublishedApi internal annotation class FilterExceptions(val mode: String = "terminate")

/**
 * Marks a class whose instances to be added to the list of leak detector candidates.
 */
@Target(AnnotationTarget.CLASS)
@PublishedApi internal annotation class LeakDetectorCandidate

/**
 * Indicates that given top level signleton object can be created in compile time and thus
 * members access doesn't need to use an init barrier and allow better optimizations for
 * field access, such as constant folding.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class CanBePrecreated
