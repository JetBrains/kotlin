/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*

internal actual fun WasmCharArray.createString(): String =
    String(null, this.len(), this)

@Suppress("NOTHING_TO_INLINE")
internal inline actual fun String.getChars() = this.chars
