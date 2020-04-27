/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

// `compareTo(x, y)` implemented as `(x >= y) - (x <= y)`

fun wasm_i32_compareTo(x: Int, y: Int): Int =
    wasm_i32_ge_s(x, y).asInt() - wasm_i32_le_s(x, y).asInt()

fun wasm_i64_compareTo(x: Long, y: Long): Int =
    wasm_i64_ge_s(x, y).asInt() - wasm_i64_le_s(x, y).asInt()

@WasmImport("runtime", "String_equals")
fun wasm_string_eq(x: String, y: String): Boolean =
    implementedAsIntrinsic