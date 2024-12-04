/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin


/**
 * A functional type builder that ensures the given [block] has a `suspend` modifier and can be
 * used as a suspend function.
 *
 * By default, functional type declarations are not inferred as `suspend` and cannot be used as such:
 *
 * ```
 * suspend fun yield() {}
 *
 * // Inferred type: '() -> Unit'
 * val regularBlock = { yield() } // Does not compile, `yield` requires to be in the suspend context
 * // Inferred type: 'suspend () -> Unit'
 * val suspendBlock = suspend { yield() } // Compiles as expected
 * ```
 */
@kotlin.internal.InlineOnly
@SinceKotlin("1.2")
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public inline fun <R> suspend(noinline block: suspend () -> R): suspend () -> R = block
