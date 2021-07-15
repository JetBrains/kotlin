/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native

import kotlin.reflect.KClass

/**
 * [SymbolName] annotation is deprecated.
 * See [KT-46649](https://youtrack.jetbrains.com/issue/KT-46649).
 */
@RequiresOptIn(
        message = "@SymbolName annotation is deprecated. See https://youtrack.jetbrains.com/issue/KT-46649",
        level = RequiresOptIn.Level.WARNING
)
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(value = AnnotationRetention.BINARY)
internal annotation class SymbolNameIsDeprecated

/**
 * This annotation is deprecated.
 * See [KT-46649](https://youtrack.jetbrains.com/issue/KT-46649).
 *
 * Forces the compiler to use specified symbol name for the target `external` function.
 *
 * TODO: changing symbol name breaks the binary compatibility,
 * so it should probably be allowed on `internal` and `private` functions only.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@SymbolNameIsDeprecated
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
 * Forces a top-level property to be initialized eagerly, opposed to lazily on the first access to file and/or property.
 * This annotation can be used as temporal migration assistance during the transition from the previous Kotlin/Native initialization scheme "eager by default"
 * to the new one, "lazy by default".
 *
 * This annotation is intended to be used only as a temporal workaround and will be removed through the regular deprecation cycle as soon as the new initialization scheme will become the default one.
 * For the usages that cannot be emulated on the new initialization scheme without this annotation, it is strongly recommended to report them during the transition period, so the proper replacement can be introduced.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@ExperimentalStdlibApi
@Deprecated("This annotation is a temporal migration assistance and may be removed in the future releases, please consider filing an issue about the case where it is needed")
public annotation class EagerInitialization

/**
 * Makes top level function available from C/C++ code with the given name.
 *
 * [externName] controls the name of top level function, [shortName] controls the short name.
 * If [externName] is empty, no top level declaration is being created.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public actual annotation class CName(actual val externName: String = "", actual val shortName: String = "")

