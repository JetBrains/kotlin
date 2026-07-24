/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.internal.staticInitializationFailure

private const val INITIALIZATION_STATE_INITIALIZED: Int = 1
private const val INITIALIZATION_STATE_ERROR: Int = 2

@UsedFromCompilerGeneratedCode
internal fun checkStaticInitializationState(state: Int, ctor: Ctor?): Boolean {
    if (state == INITIALIZATION_STATE_ERROR) {
        staticInitializationFailure(null, ctor?.`$metadata$`?.simpleName)
    }
    return state == INITIALIZATION_STATE_INITIALIZED
}
