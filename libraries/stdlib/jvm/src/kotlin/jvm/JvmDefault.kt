/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm

import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

/**
 * This annotation can no longer be used. It has been superseded by the new `-jvm-default` modes `enable` and `no-compatibility`,
 * and the annotations [JvmDefaultWithCompatibility] and [JvmDefaultWithoutCompatibility].
 */
@SinceKotlin("1.2")
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Deprecated("Switch to new -jvm-default modes: `enable` or `no-compatibility`", level = DeprecationLevel.HIDDEN)
public annotation class JvmDefault

/**
 * Prevents the compiler from generating compatibility accessors for the annotated class or interface.
 *
 * In other words, this annotation makes the compiler generate the annotated class or interface in the `-jvm-default=no-compatibility` mode.
 * For an interface, only JVM default methods are generated, without `DefaultImpls`. For a class, no implementations that call super methods
 * or `DefaultImpls` accessors are generated.
 *
 * Annotating an existing class with this annotation is a binary incompatible change.
 *
 * Used only with `-jvm-default=enable`.
 */
@SinceKotlin("1.4")
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class JvmDefaultWithoutCompatibility

/**
 * Forces the compiler to generate compatibility accessors for the annotated class or interface.
 *
 * In other words, this annotation makes the compiler generate the annotated class or interface in the `-jvm-default=enable` mode.
 * For an interface, `DefaultImpls` accessors are generated in addition to the JVM default methods. For a class, implementations that call
 * super methods are generated.

 * Used only with `-jvm-default=no-compatibility`.
 */
@SinceKotlin("1.6")
@RequireKotlin("1.6", versionKind = RequireKotlinVersionKind.COMPILER_VERSION)
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class JvmDefaultWithCompatibility
