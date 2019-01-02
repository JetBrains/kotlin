/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin.collections

/** An iterator over a sequence of values of type `UByte`. */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public abstract class UByteIterator : Iterator<UByte> {
    override final fun next() = nextUByte()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextUByte(): UByte
}

/** An iterator over a sequence of values of type `UShort`. */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public abstract class UShortIterator : Iterator<UShort> {
    override final fun next() = nextUShort()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextUShort(): UShort
}

/** An iterator over a sequence of values of type `UInt`. */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public abstract class UIntIterator : Iterator<UInt> {
    override final fun next() = nextUInt()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextUInt(): UInt
}

/** An iterator over a sequence of values of type `ULong`. */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public abstract class ULongIterator : Iterator<ULong> {
    override final fun next() = nextULong()

    /** Returns the next value in the sequence without boxing. */
    public abstract fun nextULong(): ULong
}

