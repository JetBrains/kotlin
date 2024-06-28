/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

@RequiresOptIn
private annotation class InternalWasmApi

/*
This is internal API which is not intended to use on user-side.
 */
@InternalWasmApi
public var onExportedFunctionExit: (() -> Unit)? = null

internal fun invokeOnExportedFunctionExit() {
    @OptIn(InternalWasmApi::class)
    onExportedFunctionExit?.invoke()
}