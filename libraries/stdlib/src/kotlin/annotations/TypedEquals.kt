/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * This annotation specifies that the given function is a typed equals declaration
 *
 * Any usage of a declaration annotated with `@TypedEquals` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(TypedEquals::class)`,
 * or by using the compiler argument `-opt-in=kotlin.TypedEquals`.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class TypedEquals