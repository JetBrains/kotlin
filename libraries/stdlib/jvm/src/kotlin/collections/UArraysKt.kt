/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

import kotlin.random.Random

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public object UArraysKt {

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UIntArray.random(random: Random): UInt {
        if (isEmpty())
            throw NoSuchElementException("Array is empty.")
        return get(random.nextInt(size))
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun ULongArray.random(random: Random): ULong {
        if (isEmpty())
            throw NoSuchElementException("Array is empty.")
        return get(random.nextInt(size))
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UByteArray.random(random: Random): UByte {
        if (isEmpty())
            throw NoSuchElementException("Array is empty.")
        return get(random.nextInt(size))
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UShortArray.random(random: Random): UShort {
        if (isEmpty())
            throw NoSuchElementException("Array is empty.")
        return get(random.nextInt(size))
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public infix fun UIntArray.contentEquals(other: UIntArray): Boolean {
        return storage.contentEquals(other.storage)
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public infix fun ULongArray.contentEquals(other: ULongArray): Boolean {
        return storage.contentEquals(other.storage)
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public infix fun UByteArray.contentEquals(other: UByteArray): Boolean {
        return storage.contentEquals(other.storage)
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public infix fun UShortArray.contentEquals(other: UShortArray): Boolean {
        return storage.contentEquals(other.storage)
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UIntArray.contentHashCode(): Int {
        return storage.contentHashCode()
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun ULongArray.contentHashCode(): Int {
        return storage.contentHashCode()
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UByteArray.contentHashCode(): Int {
        return storage.contentHashCode()
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UShortArray.contentHashCode(): Int {
        return storage.contentHashCode()
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UIntArray.contentToString(): String {
        return joinToString(", ", "[", "]")
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun ULongArray.contentToString(): String {
        return joinToString(", ", "[", "]")
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UByteArray.contentToString(): String {
        return joinToString(", ", "[", "]")
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UShortArray.contentToString(): String {
        return joinToString(", ", "[", "]")
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UIntArray.toTypedArray(): Array<UInt> {
        return Array(size) { index -> this[index] }
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun ULongArray.toTypedArray(): Array<ULong> {
        return Array(size) { index -> this[index] }
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UByteArray.toTypedArray(): Array<UByte> {
        return Array(size) { index -> this[index] }
    }

    @JvmStatic
    @ExperimentalUnsignedTypes
    public fun UShortArray.toTypedArray(): Array<UShort> {
        return Array(size) { index -> this[index] }
    }

}