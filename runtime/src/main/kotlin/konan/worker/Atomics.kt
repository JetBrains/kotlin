/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package konan.worker

import konan.internal.Frozen
import konan.SymbolName
import kotlinx.cinterop.NativePtr

@Frozen
class AtomicInt(private var value: Int = 0) : Number() {
    /* Atomic operations. */

    /**
     * Increments the value by [delta] and returns the new value.
     */
    @SymbolName("Kotlin_AtomicInt_addAndGet")
    external fun addAndGet(delta: Int): Int

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicInt_compareAndSwap")
    external fun compareAndSwap(expected: Int, new: Int): Int

    /**
     * Increments value by one.
     */
    fun increment(): Int = addAndGet(1)

    /**
     * Decrements value by one.
     */
    fun decrement(): Int = addAndGet(-1)

    /**
     * Returns the current value.
     */
    fun get(): Int = value

    /* Operations from Number. */

    /**
     * Returns the value of this number as a [Double], which may involve rounding.
     */
    public override fun toDouble(): Double = value.toDouble()

    /**
     * Returns the value of this number as a [Float], which may involve rounding.
     */
    public override fun toFloat(): Float = value.toFloat()

    /**
     * Returns the value of this number as a [Long], which may involve rounding or truncation.
     */
    public override fun toLong(): Long = value.toLong()

    /**
     * Returns the value of this number as an [Int], which may involve rounding or truncation.
     */
    public override fun toInt(): Int = value.toInt()

    /**
     * Returns the [Char] with the numeric value equal to this number, truncated to 16 bits if appropriate.
     */
    public override fun toChar(): Char = value.toChar()

    /**
     * Returns the value of this number as a [Short], which may involve rounding or truncation.
     */
    public override fun toShort(): Short = value.toShort()

    /**
     * Returns the value of this number as a [Byte], which may involve rounding or truncation.
     */
    public override fun toByte(): Byte = value.toByte()
}

@Frozen
class AtomicLong(private var value: Long = 0) : Number() {
    /* Atomic operations. */

    /**
     * Increments the value by [delta] and returns the new value.
     */
    @SymbolName("Kotlin_AtomicLong_addAndGet")
    external fun addAndGet(delta: Long): Long

    /**
     * Increments the value by [delta] and returns the new value.
     */
    fun addAndGet(delta: Int): Long = addAndGet(delta.toLong())

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicLong_compareAndSwap")
    external fun compareAndSwap(expected: Long, new: Long): Long

    /**
     * Increments value by one.
     */
    fun increment(): Long = addAndGet(1L)

    /**
     * Decrements value by one.
     */
    fun decrement(): Long = addAndGet(-1L)

    /**
     * Returns the current value.
     */
    fun get(): Long = value

    /* Operations from Number. */

    /**
     * Returns the value of this number as a [Double], which may involve rounding.
     */
    public override fun toDouble(): Double = value.toDouble()

    /**
     * Returns the value of this number as a [Float], which may involve rounding.
     */
    public override fun toFloat(): Float = value.toFloat()

    /**
     * Returns the value of this number as a [Long], which may involve rounding or truncation.
     */
    public override fun toLong(): Long = value.toLong()

    /**
     * Returns the value of this number as an [Int], which may involve rounding or truncation.
     */
    public override fun toInt(): Int = value.toInt()

    /**
     * Returns the [Char] with the numeric value equal to this number, truncated to 16 bits if appropriate.
     */
    public override fun toChar(): Char = value.toChar()

    /**
     * Returns the value of this number as a [Short], which may involve rounding or truncation.
     */
    public override fun toShort(): Short = value.toShort()

    /**
     * Returns the value of this number as a [Byte], which may involve rounding or truncation.
     */
    public override fun toByte(): Byte = value.toByte()
}

@Frozen
class AtomicNativePtr(private var value: NativePtr) {
    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicNativePtr_compareAndSwap")
    external fun compareAndSwap(expected: NativePtr, new: NativePtr): NativePtr

    /**
     * Returns the current value.
     */
    fun get(): NativePtr = value
}

@SymbolName("Kotlin_AtomicReference_checkIfFrozen")
external private fun checkIfFrozen(ref: Any?)

@Frozen
class AtomicReference<T>(private var value: T? = null) {
    /**
     * Creates a new atomic reference pointing to given [ref]. If reference is not frozen,
     * @InvalidMutabilityException is thrown.
     */
    init {
        checkIfFrozen(value)
    }

    /**
     * Compares value with [expected] and replaces it with [new] value if values matches.
     * If [new] value is not null, it must be frozen or permanent object, otherwise an
     * @InvalidMutabilityException is thrown.
     * Returns the old value.
     */
    @SymbolName("Kotlin_AtomicReference_compareAndSwap")
    external fun compareAndSwap(expected: T?, new: T?): T?

    /**
     * Returns the current value.
     */
    public fun get(): T? = value
}