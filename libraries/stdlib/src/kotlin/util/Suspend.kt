/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin
import kotlin.internal.RequireKotlin
import kotlin.internal.RequireKotlinVersionKind

@kotlin.internal.InlineOnly
@SinceKotlin("1.2")
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public inline fun <R> suspend(noinline block: suspend () -> R): suspend () -> R = block
