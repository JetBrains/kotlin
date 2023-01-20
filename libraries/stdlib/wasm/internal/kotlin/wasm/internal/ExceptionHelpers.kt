/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

@JsFun(
"""(message, wasmTypeName, stack) => { 
        const error = new Error();
        error.message = message;
        error.name = wasmTypeName;
        error.stack = stack;
        throw error;
   }"""
)
private external fun throwJsError(message: String?, wasmTypeName: String?, stack: String): Nothing

internal fun throwAsJsException(t: Throwable): Nothing {
    throwJsError(t.message, t::class.simpleName, t.stackTraceToString())
}

internal var isNotFirstWasmExportCall: Boolean = false