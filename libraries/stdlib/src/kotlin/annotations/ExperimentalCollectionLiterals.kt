/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.internal.JvmBuiltin

package kotlin

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HidesFromObjC

/**
 * The experimental marker for Collection Literals API.
 *
 * Any usage of a declaration marked with `@ExperimentalCollectionLiterals` must be accepted either by
 *  annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalCollectionLiterals::class)`,
 *  or by using the compiler argument `-opt-in=kotlin.annotations.ExperimentalCollectionLiterals`.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@SinceKotlin("2.3")
@OptIn(ExperimentalObjCRefinement::class)
@HidesFromObjC
public annotation class ExperimentalCollectionLiterals
