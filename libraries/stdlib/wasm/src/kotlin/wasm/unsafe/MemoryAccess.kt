/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.unsafe

import kotlin.wasm.internal.*

/**
 * Linear memory pointer type.
 * Corresponds to `i32` type on 32-bit Wasm architecture.
 */
@UnsafeWasmMemoryApi
public value class Pointer public constructor(public val address: UInt) {

    /** Adds an [Int] to the address of this [Pointer] */
    public operator fun plus(other: Int): Pointer =
        Pointer(address + other.toUInt())

    /** Subtracts an [Int] from the address of this [Pointer] */
    public operator fun minus(other: Int): Pointer =
        Pointer(address - other.toUInt())

    /** Adds an [UInt] to the address of this [Pointer] */
    public operator fun plus(other: UInt): Pointer =
        Pointer(address + other)

    /** Subtracts an [UInt] from the address of this [Pointer] */
    public operator fun minus(other: UInt): Pointer =
        Pointer(address - other)

    /** Load a Byte (8 bit) value */
    public fun loadByte(): Byte =
        wasm_i32_load8_s(address.toInt())

    /** Load a Short (16 bit) value */
    public fun loadShort(): Short =
        wasm_i32_load16_s(address.toInt())

    /** Load an Int (32 bit) value */
    public fun loadInt(): Int =
        wasm_i32_load(address.toInt())

    /** Load a Long (64 bit) value */
    public fun loadLong(): Long =
        wasm_i64_load(address.toInt())

    /** Store a Byte (8 bit) [value] */
    public fun storeByte(value: Byte): Unit =
        wasm_i32_store8(address.toInt(), value)

    /** Store a Short (16 bit) [value] */
    public fun storeShort(value: Short): Unit =
        wasm_i32_store16(address.toInt(), value)

    /** Store an Int (32 bit) [value] */
    public fun storeInt(value: Int): Unit =
        wasm_i32_store(address.toInt(), value)

    /** Store a Long (64 bit) [value] */
    public fun storeLong(value: Long): Unit =
        wasm_i64_store(address.toInt(), value)
}
