/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

/**
 * Specifies that this function is not inlined on the first stage of compilation.
 * Note: This annotation is specific for KLIB-based compilers only.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("2.3")
internal annotation class DoNotInlineOnFirstStage
