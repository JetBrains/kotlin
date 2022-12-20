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
typealias Ptr = Int

/** Load a Byte (8 bit) value from linear memory at address [ptr]. */
@WasmOp(WasmOp.I32_LOAD8_U)
public fun loadByte(ptr: Ptr): Byte =
    implementedAsIntrinsic

/** Load a Short (16 bit) value from linear memory at address [ptr]. */
@WasmOp(WasmOp.I32_LOAD16_U)
public fun loadShort(ptr: Ptr): Short =
    implementedAsIntrinsic

/** Load an Int (32 bit) value from linear memory at address [ptr]. */
@WasmOp(WasmOp.I32_LOAD)
public fun loadInt(ptr: Ptr): Int =
    implementedAsIntrinsic

/** Load a Long (64 bit) value from linear memory at address [ptr]. */
@WasmOp(WasmOp.I64_LOAD)
public fun loadLong(ptr: Ptr): Long =
    implementedAsIntrinsic

/** Store a Byte (8 bit) value into linear memory at address [ptr]. */
@WasmOp(WasmOp.I32_STORE8)
public fun storeByte(ptr: Ptr, value: Byte): Unit =
    implementedAsIntrinsic

/** Store a Short (16 bit) value into linear memory at address [ptr]. */
@WasmOp(WasmOp.I32_STORE16)
public fun storeShort(ptr: Ptr, value: Short): Unit =
    implementedAsIntrinsic

/** Store a Short (16 bit) value into linear memory at address [ptr]. */
@WasmOp(WasmOp.I32_STORE8)
public fun storeByte(ptr: Ptr, value: Int): Unit =
    implementedAsIntrinsic

/** Store an Int (32 bit) value into linear memory at address [ptr]. */
@WasmOp(WasmOp.I32_STORE)
public fun storeInt(ptr: Ptr, value: Int): Unit =
    implementedAsIntrinsic

/** Store a Long (64 bit) [value] into linear memory at address [ptr]. */
@WasmOp(WasmOp.I64_STORE)
public fun storeLong(ptr: Ptr, value: Long): Unit =
    implementedAsIntrinsic
