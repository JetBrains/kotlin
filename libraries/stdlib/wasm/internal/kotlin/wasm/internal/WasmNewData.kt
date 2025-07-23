/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

// TODO: move to WasmInstructions and remove upper bound and implementation, see a version below.
@Suppress("warnings")
internal fun <T : WasmLongArray> array_new_data(address: Int, length: Int, dataIdx: Int): T {
    return WasmLongArray(0) as T
}
//@ExcludedFromCodegen
//internal fun <T> array_new_data(address: Int, length: Int, dataIdx: Int): T = implementedAsIntrinsic