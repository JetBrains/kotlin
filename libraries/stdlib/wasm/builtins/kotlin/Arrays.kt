/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*

public class ByteArray(size: Int) {
    private var storage = WasmByteArray(size)

    public constructor(size: Int, init: (Int) -> Byte) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Byte {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Byte) {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()

    public operator fun iterator(): ByteIterator = byteArrayIterator(this)
}

internal fun byteArrayIterator(array: ByteArray) = object : ByteIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextByte() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class CharArray(size: Int) {
    private var storage = WasmCharArray(size)

    public constructor(size: Int, init: (Int) -> Char) : this(size) {
        storage.fill(size) { init(it) }
    }

    public operator fun get(index: Int): Char {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Char) {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()


    public operator fun iterator(): CharIterator = charArrayIterator(this)
}

internal fun charArrayIterator(array: CharArray) = object : CharIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextChar() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class ShortArray(size: Int) {
    private var storage = WasmShortArray(size)

    public constructor(size: Int, init: (Int) -> Short) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Short {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Short) {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()


    public operator fun iterator(): ShortIterator = shortArrayIterator(this)
}

internal fun shortArrayIterator(array: ShortArray) = object : ShortIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextShort() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class IntArray(size: Int) {
    private var storage = WasmIntArray(size)

    public constructor(size: Int, init: (Int) -> Int) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Int {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Int) {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()


    public operator fun iterator(): IntIterator = intArrayIterator(this)
}

internal fun intArrayIterator(array: IntArray) = object : IntIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextInt() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class LongArray(size: Int) {
    private var storage = WasmLongArray (size)

    public constructor(size: Int, init: (Int) -> Long) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Long {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Long) {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()

    public operator fun iterator(): LongIterator = longArrayIterator(this)
}

internal fun longArrayIterator(array: LongArray) = object : LongIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextLong() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class FloatArray(size: Int) {
    private var storage = WasmFloatArray(size)

    public constructor(size: Int, init: (Int) -> Float) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Float {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Float) {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()

    public operator fun iterator(): FloatIterator = floatArrayIterator(this)
}

internal fun floatArrayIterator(array: FloatArray) = object : FloatIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextFloat() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class DoubleArray(size: Int) {
    private var storage = WasmDoubleArray(size)

    public constructor(size: Int, init: (Int) -> Double) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Double {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Double) {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        storage.set(index, value)
    }

    public val size: Int
        get() = storage.len()

    public operator fun iterator(): DoubleIterator = doubleArrayIterator(this)
}

internal fun doubleArrayIterator(array: DoubleArray) = object : DoubleIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextDouble() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}


public class BooleanArray(size: Int) {
    private var storage = WasmByteArray(size)

    public constructor(size: Int, init: (Int) -> Boolean) : this(size) {
        storage.fill(size) { init(it).toInt().reinterpretAsByte() }
    }

    public operator fun get(index: Int): Boolean {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        return storage.get(index).reinterpretAsInt().reinterpretAsBoolean()
    }

    public operator fun set(index: Int, value: Boolean) {
        if (index < 0 || index >= storage.len()) throw IndexOutOfBoundsException()
        storage.set(index, value.toInt().reinterpretAsByte())
    }

    public val size: Int
        get() = storage.len()

    public operator fun iterator(): BooleanIterator = booleanArrayIterator(this)
}

internal fun booleanArrayIterator(array: BooleanArray) = object : BooleanIterator() {
    var index = 0
    override fun hasNext() = index != array.size
    override fun nextBoolean() = if (index != array.size) array[index++] else throw NoSuchElementException("$index")
}
