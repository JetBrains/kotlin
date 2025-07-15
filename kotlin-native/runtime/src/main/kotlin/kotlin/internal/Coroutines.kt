/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

import kotlin.coroutines.Continuation

@PublishedApi
internal actual suspend fun <T> getContinuation(): Continuation<T> = error("implemented as intrinsic")