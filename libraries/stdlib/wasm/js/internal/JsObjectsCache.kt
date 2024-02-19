/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

//Rename to getCachedJsObject after bootstrap (KT-65322)
private external fun tryGetOrSetExternrefBox(
    ref: JsAny,
    ifNotCached: JsAny
): JsAny?

internal fun getCachedJsObject(
    ref: JsAny,
    ifNotCached: JsAny
): JsAny? = tryGetOrSetExternrefBox(ref, ifNotCached)