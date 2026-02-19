/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("GeneratedCodeMarkers")

package kotlin.coroutines.jvm.internal

/* This file contains inline functions with empty bodies to mark compiler-generated code to debugger.
 *
 * The main goal is to mark compiler-generated code, so the debugger uses these markers
 * to distinguish compiler-generated code from user code without pattern-matching bytecode.
 */

// Continuation check at the beginning of a suspend function
@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal inline fun checkContinuation() {}

// Unspilling of suspend lambda arguments
@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal inline fun lambdaArgumentsUnspilling() {}

// State-machine header (TABLESWITCH of the state-machine)
@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal inline fun tableswitch() {}

// Check of $result variable at the beginning of the state-machine and after each suspend call
@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal inline fun checkResult() {}

// Check for COROUTINE_SUSPENDED marker after each suspend call
@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal inline fun checkCOROUTINE_SUSPENDED() {}

// Default label, which throws IllegalStateException - which is unreachable in normal execution
@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal inline fun unreachable() {}

