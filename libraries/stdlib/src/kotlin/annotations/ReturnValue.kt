/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Expresses that calls of annotated function are ignorable.
 * Ignorable calls would not trigger a warning from Kotlin's return value checker, even if they are not used.
 *
 * This annotation only makes sense when used together with 'Return value checker' feature.
 * Placing it without enabling the corresponding feature would result in a compiler error.
 */
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class IgnorableReturnValue

/**
 * Marks the *scope* (file or class) in which all the functions are non-ignorable.
 * Kotlin's return value checker would report warning in case the result of non-ignorable function call is not used.
 *
 * This annotation only makes sense when used together with 'Return value checker' feature.
 * Placing it without enabling the corresponding feature would result in a compiler error.
 */
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS)
public annotation class MustUseReturnValue


