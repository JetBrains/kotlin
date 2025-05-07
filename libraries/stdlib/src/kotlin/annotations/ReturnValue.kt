/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Expresses that calls of the annotated function are ignorable.
 * Ignorable calls would not trigger a warning from Kotlin's return value checker, even if they are not used.
 *
 * This annotation only makes sense when used together with the 'Return value checker' feature.
 * Placing it without enabling the corresponding feature would result in a compiler error.
 *
 * Please note that this feature is currently experimental and this annotation may be changed without further notice
 * or deprecation cycle. This, in particular, concerns annotation's target list.
 */
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
@SinceKotlin("2.2")
public annotation class IgnorableReturnValue

/**
 * Marks the *scope* (file or class) in which all the functions are non-ignorable.
 * Kotlin's return value checker would report a warning in case the result of a non-ignorable function call is not used.
 *
 * This annotation is usually placed by the Kotlin compiler itself when the corresponding 'Return value checker' feature
 * is set to the 'full' mode. There is no need to place it manually except for certain migration scenarios.
 *
 * This annotation only makes sense when used together with the 'Return value checker' feature.
 * Placing it without enabling the corresponding feature would result in a compiler error.
 *
 * Please note that this feature is currently experimental and this annotation may be changed without further notice
 * or deprecation cycle. This, in particular, concerns annotation's target list.
 */
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS)
@SinceKotlin("2.2")
public annotation class MustUseReturnValue
