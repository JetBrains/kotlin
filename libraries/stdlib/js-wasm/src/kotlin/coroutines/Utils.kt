/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

/**
 * A wrapper around `try`/`catch` to work around the fact that we can't catch `dynamic` in Kotlin/Wasm, as `dynamic` is not available
 * there.
 */
internal expect inline fun tryCatchAll(tryBlock: () -> Unit, catchBlock: (Throwable) -> Unit)
