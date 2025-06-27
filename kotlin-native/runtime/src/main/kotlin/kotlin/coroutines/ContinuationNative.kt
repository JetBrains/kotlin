/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

import kotlin.internal.InlineOnly
import kotlin.native.internal.getContinuation

@SinceKotlin(version = "1.3")
@Suppress("WRONG_MODIFIER_TARGET")
@InlineOnly
public actual suspend inline val coroutineContext: CoroutineContext
    get() = getContinuation<Any?>().context
