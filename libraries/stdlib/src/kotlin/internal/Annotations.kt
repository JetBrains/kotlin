/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

/**
 * Specifies that the corresponding type should be ignored during type inference.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
internal annotation class NoInfer

/**
 * Specifies that the constraint built for the type during type inference should be an equality one.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
internal annotation class Exact

/**
 * Specifies that a corresponding member has the lowest priority in overload resolution.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class LowPriorityInOverloadResolution

/**
 * Specifies that the corresponding member has the highest priority in overload resolution. Effectively this means that
 * an extension annotated with this annotation will win in overload resolution over a member with the same signature.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class HidesMembers

/**
 * The value of this type parameter should be mentioned in input types (argument types, receiver type or expected type).
 */
@Target(AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class OnlyInputTypes

/**
 * Specifies that this function should not be called directly without inlining
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
internal annotation class InlineOnly

/**
 * Specifies that this declaration can have dynamic receiver type.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class DynamicExtension

/**
 * The value of this parameter should be a property reference expression (`this::foo`), referencing a `lateinit` property,
 * the backing field of which is accessible at the point where the corresponding argument is passed.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.2")
internal annotation class AccessibleLateinitPropertyLiteral

/**
 * Specifies that this declaration is only completely supported since the specified version.
 *
 * The Kotlin compiler of an earlier version is going to report a diagnostic on usages of this declaration.
 * The diagnostic message can be specified with [message], or via [errorCode] (takes less space, but might not be immediately clear
 * to the user). The diagnostic severity can be specified with [level]: WARNING/ERROR mean that either a warning or an error
 * is going to be reported, HIDDEN means that the declaration is going to be removed from resolution completely.
 *
 * [versionKind] specifies which version should be compared with the [version] value, when compiling the usage of the annotated declaration.
 * Note that prior to 1.2, only [RequireKotlinVersionKind.LANGUAGE_VERSION] was supported, so the Kotlin compiler before 1.2 is going to
 * treat any [RequireKotlin] as if it requires the language version. Since 1.2, the Kotlin compiler supports
 * [RequireKotlinVersionKind.LANGUAGE_VERSION], [RequireKotlinVersionKind.COMPILER_VERSION] and [RequireKotlinVersionKind.API_VERSION].
 * If the actual value of [versionKind] is something different (e.g. a new version kind, added in future versions of Kotlin),
 * Kotlin 1.2 is going to ignore this [RequireKotlin] altogether, where as Kotlin before 1.2 is going to treat this as a requirement
 * on the language version.
 *
 * This annotation is erased at compile time; its arguments are stored in a more compact form in the Kotlin metadata.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.TYPEALIAS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
@SinceKotlin("1.2")
internal annotation class RequireKotlin(
    val version: String,
    val message: String = "",
    val level: DeprecationLevel = DeprecationLevel.ERROR,
    val versionKind: RequireKotlinVersionKind = RequireKotlinVersionKind.LANGUAGE_VERSION,
    val errorCode: Int = -1
)

/**
 * The kind of the version that is required by [RequireKotlin].
 */
@SinceKotlin("1.2")
internal enum class RequireKotlinVersionKind {
    LANGUAGE_VERSION,
    COMPILER_VERSION,
    API_VERSION,
}

/**
 * Specifies that this declaration is a part of special DSL, used for constructing function's contract.
 */
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.2")
internal annotation class ContractsDsl
