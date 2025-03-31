/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

/**
 * Specifies that the corresponding type parameter is not used for unsafe operations such as casts or 'is' checks
 * That means it's completely safe to use generic types as argument for such parameter.
 */
@Target(AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal annotation class PureReifiable

/**
 * Specifies that the corresponding built-in method exists depending on platform.
 * Current implementation for JVM looks whether method with same JVM descriptor exists in the module JDK.
 * For example MutableMap.remove(K, V) available only if corresponding
 * method 'java/util/Map.remove(Ljava/lang/Object;Ljava/lang/Object;)Z' is defined in JDK (i.e. for major versions >= 8)
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class PlatformDependent

/**
 * When applied to a function or property, enables a compiler optimization that evaluates that function or property
 * at compile-time and replaces calls to it with the computed result.
 */
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("1.7")
internal annotation class IntrinsicConstEvaluation
