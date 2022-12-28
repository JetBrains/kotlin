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
@UnsafeWasmMemoryApi
value class Pointer internal constructor(private val address: Int) {
    /** Converts [Pointer] to an [Int] memory address */
    public fun toInt(): Int = address

    /** Adds an [Int] to the address of this [Pointer] */
    public operator fun plus(other: Int): Pointer =
        Pointer(address + other)

    /** Subtracts an [Int] from the address of this [Pointer] */
    public operator fun minus(other: Int): Pointer =
        Pointer(address - other)

    /** Load a Byte (8 bit) value */
    @WasmOp(WasmOp.I32_LOAD8_S)
    public fun loadByte(): Byte =
        implementedAsIntrinsic

    /** Load a Short (16 bit) value */
    @WasmOp(WasmOp.I32_LOAD16_S)
    public fun loadShort(): Short =
        implementedAsIntrinsic

    /** Load an Int (32 bit) value */
    @WasmOp(WasmOp.I32_LOAD)
    public fun loadInt(): Int =
        implementedAsIntrinsic

    /** Load a Long (64 bit) value */
    @WasmOp(WasmOp.I64_LOAD)
    public fun loadLong(): Long =
        implementedAsIntrinsic

    /** Store a Byte (8 bit) [value] */
    @WasmOp(WasmOp.I32_STORE8)
    public fun storeByte(value: Byte): Unit =
        implementedAsIntrinsic

    /** Store a Short (16 bit) [value] */
    @WasmOp(WasmOp.I32_STORE16)
    public fun storeShort(value: Short): Unit =
        implementedAsIntrinsic

    /** Store an Int (32 bit) [value] */
    @WasmOp(WasmOp.I32_STORE)
    public fun storeInt(value: Int): Unit =
        implementedAsIntrinsic

    /** Store a Long (64 bit) [value] */
    @WasmOp(WasmOp.I64_STORE)
    public fun storeLong(value: Long): Unit =
        implementedAsIntrinsic
}

/**
 * Converts this [Int] memory address into a [Pointer]
 */
@UnsafeWasmMemoryApi
public fun Int.toPointer() = Pointer(this)