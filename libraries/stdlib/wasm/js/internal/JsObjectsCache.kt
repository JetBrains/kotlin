/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    function getCachedJsObject(ref, ifNotCached) {
        if (getCachedJsObject.cachedJsObjects == undefined) {
            getCachedJsObject.cachedJsObjects = new WeakMap();
        }
        if (typeof ref !== 'object' && typeof ref !== 'function') return ifNotCached;
        const cached = getCachedJsObject.cachedJsObjects.get(ref);
        if (cached !== void 0) return cached;
        getCachedJsObject.cachedJsObjects.set(ref, ifNotCached);
        return ifNotCached;
    }
"""
)
internal external fun getCachedJsObjectWasm(
    ref: JsAny,
    ifNotCached: JsAny
): JsAny?

@OptIn(ExperimentalWasmJsInterop::class, ExperimentalJsExport::class)
@JsExport
public fun getCachedJsObject(
    ref: JsAny,
    ifNotCached: JsAny
): JsAny? =
    getCachedJsObjectWasm(ref, ifNotCached)
