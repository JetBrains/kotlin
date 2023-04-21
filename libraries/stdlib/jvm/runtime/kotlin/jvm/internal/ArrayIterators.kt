/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin.jvm.internal

private class ArrayByteIterator(private val array: ByteArray) : ByteIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextByte() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}
private class VArrayByteIterator(private val array: VArray<Byte>) : ByteIterator(), VArrayIterator<Byte> {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextByte() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayCharIterator(private val array: CharArray) : CharIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextChar() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}
private class VArrayCharIterator(private val array: VArray<Char>) : CharIterator(), VArrayIterator<Char> {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextChar() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayShortIterator(private val array: ShortArray) : ShortIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextShort() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}
private class VArrayShortIterator(private val array: VArray<Short>) : ShortIterator(), VArrayIterator<Short> {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextShort() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayIntIterator(private val array: IntArray) : IntIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextInt() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}
private class VArrayIntIterator(private val array: VArray<Int>) : IntIterator(), VArrayIterator<Int> {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextInt() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayLongIterator(private val array: LongArray) : LongIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextLong() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}
private class VArrayLongIterator(private val array: VArray<Long>) : LongIterator(), VArrayIterator<Long> {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextLong() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayFloatIterator(private val array: FloatArray) : FloatIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextFloat() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}
private class VArrayFloatIterator(private val array: VArray<Float>) : FloatIterator(), VArrayIterator<Float> {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextFloat() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayDoubleIterator(private val array: DoubleArray) : DoubleIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextDouble() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}
private class VArrayDoubleIterator(private val array: VArray<Double>) : DoubleIterator(), VArrayIterator<Double> {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextDouble() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

private class ArrayBooleanIterator(private val array: BooleanArray) : BooleanIterator() {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextBoolean() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}
private class VArrayBooleanIterator(private val array: VArray<Boolean>) : BooleanIterator(), VArrayIterator<Boolean> {
    private var index = 0
    override fun hasNext() = index < array.size
    override fun nextBoolean() = try { array[index++] } catch (e: ArrayIndexOutOfBoundsException) { index -= 1; throw NoSuchElementException(e.message) }
}

public fun iterator(array: ByteArray): ByteIterator = ArrayByteIterator(array)
public fun vArrayIterator(array: VArray<Byte>): ByteIterator = VArrayByteIterator(array)
public fun iterator(array: CharArray): CharIterator = ArrayCharIterator(array)
public fun vArrayIterator(array: VArray<Char>): CharIterator = VArrayCharIterator(array)
public fun iterator(array: ShortArray): ShortIterator = ArrayShortIterator(array)
public fun vArrayIterator(array: VArray<Short>): ShortIterator = VArrayShortIterator(array)
public fun iterator(array: IntArray): IntIterator = ArrayIntIterator(array)
public fun vArrayIterator(array: VArray<Int>): IntIterator = VArrayIntIterator(array)
public fun iterator(array: LongArray): LongIterator = ArrayLongIterator(array)
public fun vArrayIterator(array: VArray<Long>): LongIterator = VArrayLongIterator(array)
public fun iterator(array: FloatArray): FloatIterator = ArrayFloatIterator(array)
public fun vArrayIterator(array: VArray<Float>): FloatIterator = VArrayFloatIterator(array)
public fun iterator(array: DoubleArray): DoubleIterator = ArrayDoubleIterator(array)
public fun vArrayIterator(array: VArray<Double>): DoubleIterator = VArrayDoubleIterator(array)
public fun iterator(array: BooleanArray): BooleanIterator = ArrayBooleanIterator(array)
public fun vArrayIterator(array: VArray<Boolean>): BooleanIterator = VArrayBooleanIterator(array)
