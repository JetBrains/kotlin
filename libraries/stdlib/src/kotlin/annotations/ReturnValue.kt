/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:kotlin.internal.JvmBuiltin
package kotlin

/**
 * Expresses that calls of the annotated function are ignorable.
 * Ignorable calls would not trigger a warning from Kotlin's [return value checker](https://kotlinlang.org/docs/unused-return-value-checker.html), even if they are not used.
 *
 * This annotation only makes sense when used together with the 'Return value checker' feature.
 * Please note that this feature is available as stable only since Kotlin 2.5.
 * Using this annotation with a lower language version requires an explicit opt-in into the feature.
 */
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
@SinceKotlin("2.3")
public annotation class IgnorableReturnValue

/**
 * Marks the *scope* (file or class) in which all the functions are non-ignorable.
 * Kotlin's [return value checker](https://kotlinlang.org/docs/unused-return-value-checker.html) would report a warning in case the result of a non-ignorable function call is not used.
 *
 * This annotation is usually placed by the Kotlin compiler itself when the checker
 * is set to the 'full' mode. There is no need to place it manually except for certain migration scenarios.
 *
 * This annotation only makes sense when used together with the 'Return value checker' feature.
 * Please note that this feature is available as stable only since Kotlin 2.5.
 * Using this annotation with a lower language version requires an explicit opt-in into the feature.
 */
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS)
@SinceKotlin("2.3")
public annotation class MustUseReturnValues
