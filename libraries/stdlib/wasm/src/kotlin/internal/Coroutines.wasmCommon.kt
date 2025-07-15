/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FILE
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.coroutines.Continuation

@Target(FILE, CLASS, FUNCTION, CONSTRUCTOR, PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class ExcludedFromCodegen

@ExcludedFromCodegen
@PublishedApi
internal actual suspend fun <T> getContinuation(): Continuation<T> =
    kotlin.wasm.internal.getContinuation()