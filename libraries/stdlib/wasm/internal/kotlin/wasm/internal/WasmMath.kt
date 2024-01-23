/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

// `compareTo(x, y)` implemented as `(x >= y) - (x <= y)`

@PublishedApi
internal fun wasm_i32_compareTo(x: Int, y: Int): Int =
    wasm_i32_ge_s(x, y).toInt() - wasm_i32_le_s(x, y).toInt()

@PublishedApi
internal fun wasm_u32_compareTo(x: Int, y: Int): Int =
    wasm_i32_ge_u(x, y).toInt() - wasm_i32_le_u(x, y).toInt()

@PublishedApi
internal fun wasm_i64_compareTo(x: Long, y: Long): Int =
    wasm_i64_ge_s(x, y).toInt() - wasm_i64_le_s(x, y).toInt()

@PublishedApi
internal fun wasm_u64_compareTo(x: Long, y: Long): Int =
    wasm_i64_ge_u(x, y).toInt() - wasm_i64_le_u(x, y).toInt()
