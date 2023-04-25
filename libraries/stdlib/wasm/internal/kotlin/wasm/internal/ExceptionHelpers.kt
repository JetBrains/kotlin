/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

@Suppress("UNUSED_PARAMETER") // TODO: Remove after bootstrap update
private fun throwJsError(message: String?, wasmTypeName: String?, stack: ExternalInterfaceType): Nothing {
    js("""
    const error = new Error();
    error.message = message;
    error.name = wasmTypeName;
    error.stack = stack;
    throw error;
    """)
}

internal fun throwAsJsException(t: Throwable): Nothing {
    throwJsError(t.message, getSimpleName(t.typeInfo), t.jsStack)
}

internal var isNotFirstWasmExportCall: Boolean = false