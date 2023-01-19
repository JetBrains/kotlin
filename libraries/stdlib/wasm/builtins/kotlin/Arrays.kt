/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.wasm.internal.*

public class ByteArray(size: Int) {
    internal val storage = WasmByteArray(size)

    @Suppress("TYPE_PARAMETER_AS_REIFIED", "UNUSED_PARAMETER", "CAST_NEVER_SUCCEEDS")
    @WasmPrimitiveConstructor
    internal constructor(storage: WasmByteArray) : this(check(false) as Int)

    public constructor(size: Int, init: (Int) -> Byte) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Byte {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Byte) {
        rangeCheck(index, storage.len())
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
    internal val storage = WasmCharArray(size)

    @Suppress("TYPE_PARAMETER_AS_REIFIED", "UNUSED_PARAMETER", "CAST_NEVER_SUCCEEDS")
    @WasmPrimitiveConstructor
    internal constructor(storage: WasmCharArray) : this(check(false) as Int)

    public constructor(size: Int, init: (Int) -> Char) : this(size) {
        storage.fill(size) { init(it) }
    }

    public operator fun get(index: Int): Char {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Char) {
        rangeCheck(index, storage.len())
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
    internal val storage = WasmShortArray(size)

    @Suppress("TYPE_PARAMETER_AS_REIFIED", "UNUSED_PARAMETER", "CAST_NEVER_SUCCEEDS")
    @WasmPrimitiveConstructor
    internal constructor(storage: WasmShortArray) : this(check(false) as Int)

    public constructor(size: Int, init: (Int) -> Short) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Short {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Short) {
        rangeCheck(index, storage.len())
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
    internal val storage = WasmIntArray(size)

    @Suppress("TYPE_PARAMETER_AS_REIFIED", "UNUSED_PARAMETER", "CAST_NEVER_SUCCEEDS")
    @WasmPrimitiveConstructor
    internal constructor(storage: WasmIntArray) : this(check(false) as Int)

    public constructor(size: Int, init: (Int) -> Int) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Int {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Int) {
        rangeCheck(index, storage.len())
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
    internal val storage = WasmLongArray (size)

    @Suppress("TYPE_PARAMETER_AS_REIFIED", "UNUSED_PARAMETER", "CAST_NEVER_SUCCEEDS")
    @WasmPrimitiveConstructor
    internal constructor(storage: WasmLongArray) : this(check(false) as Int)

    public constructor(size: Int, init: (Int) -> Long) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Long {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Long) {
        rangeCheck(index, storage.len())
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
    internal val storage = WasmFloatArray(size)

    @Suppress("TYPE_PARAMETER_AS_REIFIED", "UNUSED_PARAMETER", "CAST_NEVER_SUCCEEDS")
    @WasmPrimitiveConstructor
    internal constructor(storage: WasmFloatArray) : this(check(false) as Int)

    public constructor(size: Int, init: (Int) -> Float) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Float {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Float) {
        rangeCheck(index, storage.len())
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
    internal val storage = WasmDoubleArray(size)

    @Suppress("TYPE_PARAMETER_AS_REIFIED", "UNUSED_PARAMETER", "CAST_NEVER_SUCCEEDS")
    @WasmPrimitiveConstructor
    internal constructor(storage: WasmDoubleArray) : this(check(false) as Int)

    public constructor(size: Int, init: (Int) -> Double) : this(size) {
        storage.fill(size, init)
    }

    public operator fun get(index: Int): Double {
        rangeCheck(index, storage.len())
        return storage.get(index)
    }

    public operator fun set(index: Int, value: Double) {
        rangeCheck(index, storage.len())
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
    internal val storage = WasmByteArray(size)

    @Suppress("TYPE_PARAMETER_AS_REIFIED", "CAST_NEVER_SUCCEEDS", "UNUSED_PARAMETER")
    @WasmPrimitiveConstructor
    internal constructor(storage: WasmByteArray) : this(check(false) as Int)

    public constructor(size: Int, init: (Int) -> Boolean) : this(size) {
        storage.fill(size) { init(it).toInt().reinterpretAsByte() }
    }

    public operator fun get(index: Int): Boolean {
        rangeCheck(index, storage.len())
        return storage.get(index).reinterpretAsInt().reinterpretAsBoolean()
    }

    public operator fun set(index: Int, value: Boolean) {
        rangeCheck(index, storage.len())
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
