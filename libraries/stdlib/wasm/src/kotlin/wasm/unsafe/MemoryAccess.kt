/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.unsafe

import kotlin.wasm.internal.WasmOp
import kotlin.wasm.internal.implementedAsIntrinsic

/**
 * Linear memory pointer type.
 * Corresponds to `i32` type on 32-bit Wasm architecture.
 */
typealias Pointer = Int

/** Load a Byte (8 bit) value from linear memory at address [pointer]. */
@WasmOp(WasmOp.I32_LOAD8_S)
@UnsafeWasmMemoryApi
public fun loadByte(pointer: Pointer): Byte =
    implementedAsIntrinsic

/** Load a Short (16 bit) value from linear memory at address [pointer]. */
@WasmOp(WasmOp.I32_LOAD16_S)
@UnsafeWasmMemoryApi
public fun loadShort(pointer: Pointer): Short =
    implementedAsIntrinsic

/** Load an Int (32 bit) value from linear memory at address [pointer]. */
@WasmOp(WasmOp.I32_LOAD)
@UnsafeWasmMemoryApi
public fun loadInt(pointer: Pointer): Int =
    implementedAsIntrinsic

/** Load a Long (64 bit) value from linear memory at address [pointer]. */
@WasmOp(WasmOp.I64_LOAD)
@UnsafeWasmMemoryApi
public fun loadLong(pointer: Pointer): Long =
    implementedAsIntrinsic

/** Store a Byte (8 bit) value into linear memory at address [pointer]. */
@WasmOp(WasmOp.I32_STORE8)
@UnsafeWasmMemoryApi
public fun storeByte(pointer: Pointer, value: Byte): Unit =
    implementedAsIntrinsic

/** Store a Short (16 bit) value into linear memory at address [pointer]. */
@WasmOp(WasmOp.I32_STORE16)
@UnsafeWasmMemoryApi
public fun storeShort(pointer: Pointer, value: Short): Unit =
    implementedAsIntrinsic

/** Store an Int (32 bit) value into linear memory at address [pointer]. */
@WasmOp(WasmOp.I32_STORE)
@UnsafeWasmMemoryApi
public fun storeInt(pointer: Pointer, value: Int): Unit =
    implementedAsIntrinsic

/** Store a Long (64 bit) [value] into linear memory at address [pointer]. */
@WasmOp(WasmOp.I64_STORE)
@UnsafeWasmMemoryApi
public fun storeLong(pointer: Pointer, value: Long): Unit =
    implementedAsIntrinsic
