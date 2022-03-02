/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

// This is called when exported function returns a string. It writes [i32 length, [i16 chars ...]] into a temporary raw memory area and
// returns pointer to the start of it.
// Note: currently there is a single temporary raw memory area so it's not possible to export more than one string at a time.
internal fun exportString(src: String?): Int {
    if (src == null)
        throw IllegalArgumentException("Exporting null string")

    val retAddr = unsafeGetScratchRawMemory(INT_SIZE_BYTES + src.length * CHAR_SIZE_BYTES)
    wasm_i32_store(retAddr, src.length)
    unsafeWasmCharArrayToRawMemory(src.chars, retAddr + INT_SIZE_BYTES)
    return retAddr
}

// See importStringToJs for the JS-side import for strings