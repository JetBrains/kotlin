/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.experimental.ExperimentalNativeApi

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
@PublishedApi
internal annotation class ExportForCppRuntime(val name: String = "")

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
@PublishedApi
internal annotation class ExportForCompiler

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
@PublishedApi
internal annotation class ExportTypeInfo(val name: String)

/**
 * If a lambda shall be carefully lowered by the compiler.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class VolatileLambda

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

@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
internal annotation class ConstantConstructorIntrinsic(val kind: String)

/**
 * Indicates that `@SymbolName external` function is implemented in library-stored bitcode
 * and doesn't have native dependencies.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@PublishedApi
internal annotation class Independent

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
 * Indicates that given top level singleton object can be created in compile time and thus
 * members access doesn't need to use an init barrier and allow better optimizations for
 * field access, such as constant folding.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@PublishedApi
internal actual annotation class CanBePrecreated

/**
 * Marks a class that has a finalizer.
 */
@Target(AnnotationTarget.CLASS)
internal annotation class HasFinalizer

/**
 * Marks a declaration that is internal for Kotlin/Native and shouldn't be used externally.
 */
@Target(
        AnnotationTarget.CLASS,
        AnnotationTarget.ANNOTATION_CLASS,
        AnnotationTarget.PROPERTY,
        AnnotationTarget.FIELD,
        AnnotationTarget.LOCAL_VARIABLE,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.TYPEALIAS
)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(value = AnnotationRetention.BINARY)
internal annotation class InternalForKotlinNative

/**
 * Indicates that calls of this function will be replaced with calls to the
 * [callee] implemented in the C++ part of the runtime.
 *
 * This annotation is unsafe and should be used with care: [callee] is
 * responsible for correct interaction with the garbage collector, like
 * placing safe points and switching thread state when using blocking APIs.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(value = AnnotationRetention.BINARY)
@InternalForKotlinNative
public annotation class GCUnsafeCall(val callee: String)

/**
 * Marks a declaration that is internal for Kotlin/Native tests and shouldn't be used externally.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(value = AnnotationRetention.BINARY)
internal annotation class InternalForKotlinNativeTests

@InternalForKotlinNativeTests
@Target(AnnotationTarget.FILE)
public annotation class ReflectionPackageName(val name: String)

/**
 * Indicates that the marked function is an exported bridge between Kotlin and the platform.
 * This annotation prevents the function from being removed by DCE
 * and specifies a stable [bridgeName] for the function symbol.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(value = AnnotationRetention.BINARY)
@ExperimentalNativeApi
public annotation class ExportedBridge(val bridgeName: String)
