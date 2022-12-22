/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * This annotation specifies that the given function is a typed equals declaration.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class TypedEquals

/**
 * This annotation allows use @TypedEquals annotations inside given class declaration
 *
 * Any usage of a class annotated with `@AllowTypedEquals` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(AllowTypedEquals::class)`,
 * or by using the compiler argument `-opt-in=kotlin.AllowTypedEquals`
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@RequiresOptIn
annotation class AllowTypedEquals