/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Marks API related to interoperability with JS as experimental.
 *
 * Note that the behavior of such API may be changed in the future.
 *
 * Usages of such API will be reported as warnings unless an explicit opt-in with
 * the [OptIn] annotation, e.g. `@OptIn(ExperimentalWasmJsInterop::class)`,
 * or with the `-opt-in=kotlin.js.ExperimentalWasmJsInterop` compiler option is given.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("2.2")
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS
)
public annotation class ExperimentalWasmJsInterop
