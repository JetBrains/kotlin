/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.internal.JvmBuiltin

package kotlin

import kotlin.annotation.AnnotationTarget.*


/**
 * This annotation marks the experimental API supporting collection literals in companion blocks of Kotlin standard collections.
 *
 * Note that in order to use collection literals for Kotlin standard collections with new experimental `operator fun of` functions
 * in collection companion blocks, you need to enable the corresponding experimental features
 * by specifying the compiler arguments `-Xcollection-literals` and `-Xcompanion-blocks`.
 *
 * Enabling `-Xcollection-literals` automatically opts in to this experimental API.
 * Otherwise, any usage of a declaration annotated with `@ExperimentalCollectionLiteralsApi` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalCollectionLiteralsApi::class)`,
 * or by using the compiler argument `-opt-in=kotlin.ExperimentalCollectionLiteralsApi`.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(
    CLASS,
    ANNOTATION_CLASS,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPEALIAS
)
@MustBeDocumented
@SinceKotlin("2.5")
public annotation class ExperimentalCollectionLiteralsApi
