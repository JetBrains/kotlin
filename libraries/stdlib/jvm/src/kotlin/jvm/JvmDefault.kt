/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

/**
 *
 * Specifies that a JVM default method should be generated for non-abstract Kotlin interface member.
 *
 * Usages of this annotation require an explicit compilation argument to be specified:
 * either `-Xjvm-default=enable` or `-Xjvm-default=compatibility`.
 *
 * * with `-Xjvm-default=enable`, only default method in interface is generated for each @[JvmDefault] method.
 *   In this mode, annotating an existing method with @[JvmDefault] can break binary compatibility, because it will effectively
 *   remove the method from the `DefaultImpls` class.
 * * with `-Xjvm-default=compatibility`, in addition to the default interface method, a compatibility accessor is generated
 *   in the `DefaultImpls` class, that calls the default interface method via a synthetic accessor.
 *   In this mode, annotating an existing method with @[JvmDefault] is binary compatible, but results in more methods in bytecode.
 *
 * Removing this annotation from an interface member is a binary incompatible change in both modes.
 *
 * Generation of default methods is only possible with JVM target bytecode version 1.8 (`-jvm-target 1.8`) or higher.
 *
 * @[JvmDefault] methods are excluded from interface delegation.
 */
@SinceKotlin("1.2")
@RequireKotlin("1.2.40", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Deprecated("Switch to new -Xjvm-default modes: `all` or `all-compatibility`")
annotation class JvmDefault

/**
 * Prevents the compiler from generating compatibility accessors for the annotated class or interface, and suppresses
 * any related compatibility warnings. In other words, this annotation makes the compiler generate the annotated class
 * or interface in the `-Xjvm-default=all` mode, where only JVM default methods are generated, without `DefaultImpls`.
 *
 * Annotating an existing class with this annotation is a binary incompatible change. Therefore this annotation makes
 * the most sense for _new_ classes in libraries which opted into the compatibility mode.
 *
 * Used only with `-Xjvm-default=compatibility|all-compatibility`.
 */
@SinceKotlin("1.4")
@RequireKotlin("1.4", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
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