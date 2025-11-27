/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*

internal expect fun WasmCharArray.createString(): String

/**
 * For WasmJs target returns a newly created array by calling js-builtin function.
 *
 * For WasmWasi target returns a content of an internal String field, that stores WasmCharArray.
 * Mutating it will change the contents of the String.
 * When calling this function in WasmWasi and common Wasm code, it's recommended to copy the contents of the array.
 */
internal expect fun String.getChars(): WasmCharArray
