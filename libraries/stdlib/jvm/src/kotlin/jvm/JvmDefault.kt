/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

/**
 * Specifies that a JVM default method should be generated for non-abstract Kotlin interface member.
 *
 * This annotation can no longer be used. It has been superseded by the new `-Xjvm-default` modes `all` and `all-compatibility`,
 * and the new annotations `@JvmDefaultWithCompatibility` and `@JvmDefaultWithoutCompatibility`.
 */
@SinceKotlin("1.2")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Deprecated("Switch to new -Xjvm-default modes: `all` or `all-compatibility`", level = DeprecationLevel.ERROR)
annotation class JvmDefault

/**
 * Prevents the compiler from generating compatibility accessors for the annotated class or interface, and suppresses
 * any related compatibility warnings. In other words, this annotation makes the compiler generate the annotated class
 * or interface in the `-Xjvm-default=all` mode, where only JVM default methods are generated, without `DefaultImpls`.
 *
 * Annotating an existing class with this annotation is a binary incompatible change. Therefore this annotation makes
 * the most sense for _new_ classes in libraries which opted into the compatibility mode.
 *
 * Used only with `-Xjvm-default=all-compatibility`.
 */
@SinceKotlin("1.4")
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class JvmDefaultWithoutCompatibility

/**
 * Forces the compiler to generate compatibility accessors for the annotated interface in the `DefaultImpls` class.
 * Please note that if an interface is annotated with this annotation for binary compatibility, public derived Kotlin interfaces should also be annotated with it,
 * because their `DefaultImpls` methods will be used to access implementations from the `DefaultImpls` class of the original interface.
 *
 * Used only with `-Xjvm-default=all`. For more details refer to `-Xjvm-default` documentation.
 */
@SinceKotlin("1.6")
@RequireKotlin("1.6", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class JvmDefaultWithCompatibility
