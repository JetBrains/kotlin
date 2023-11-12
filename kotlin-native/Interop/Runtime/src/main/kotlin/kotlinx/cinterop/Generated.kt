/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FINAL_UPPER_BOUND", "NOTHING_TO_INLINE")

package kotlinx.cinterop

@ExperimentalForeignApi
@JvmName("plus\$Byte")
public inline operator fun <T : ByteVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 1)

@ExperimentalForeignApi
@JvmName("plus\$Byte")
public inline operator fun <T : ByteVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@ExperimentalForeignApi
@JvmName("get\$Byte")
public inline operator fun <T : Byte> CPointer<ByteVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Byte")
public inline operator fun <T : Byte> CPointer<ByteVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("get\$Byte")
public inline operator fun <T : Byte> CPointer<ByteVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Byte")
public inline operator fun <T : Byte> CPointer<ByteVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("plus\$Short")
public inline operator fun <T : ShortVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 2)

@ExperimentalForeignApi
@JvmName("plus\$Short")
public inline operator fun <T : ShortVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@ExperimentalForeignApi
@JvmName("get\$Short")
public inline operator fun <T : Short> CPointer<ShortVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Short")
public inline operator fun <T : Short> CPointer<ShortVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("get\$Short")
public inline operator fun <T : Short> CPointer<ShortVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Short")
public inline operator fun <T : Short> CPointer<ShortVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("plus\$Int")
public inline operator fun <T : IntVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 4)

@ExperimentalForeignApi
@JvmName("plus\$Int")
public inline operator fun <T : IntVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@ExperimentalForeignApi
@JvmName("get\$Int")
public inline operator fun <T : Int> CPointer<IntVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Int")
public inline operator fun <T : Int> CPointer<IntVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("get\$Int")
public inline operator fun <T : Int> CPointer<IntVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Int")
public inline operator fun <T : Int> CPointer<IntVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("plus\$Long")
public inline operator fun <T : LongVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 8)

@ExperimentalForeignApi
@JvmName("plus\$Long")
public inline operator fun <T : LongVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@ExperimentalForeignApi
@JvmName("get\$Long")
public inline operator fun <T : Long> CPointer<LongVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Long")
public inline operator fun <T : Long> CPointer<LongVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("get\$Long")
public inline operator fun <T : Long> CPointer<LongVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Long")
public inline operator fun <T : Long> CPointer<LongVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("plus\$UByte")
public inline operator fun <T : UByteVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 1)

@ExperimentalForeignApi
@JvmName("plus\$UByte")
public inline operator fun <T : UByteVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@ExperimentalForeignApi
@JvmName("get\$UByte")
public inline operator fun <T : UByte> CPointer<UByteVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
public inline operator fun <T : UByte> CPointer<UByteVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
public inline operator fun <T : UByte> CPointer<UByteVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
public inline operator fun <T : UByte> CPointer<UByteVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("plus\$UShort")
public inline operator fun <T : UShortVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 2)

@ExperimentalForeignApi
@JvmName("plus\$UShort")
public inline operator fun <T : UShortVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@ExperimentalForeignApi
@JvmName("get\$UShort")
public inline operator fun <T : UShort> CPointer<UShortVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
public inline operator fun <T : UShort> CPointer<UShortVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("get\$UShort")
public inline operator fun <T : UShort> CPointer<UShortVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
public inline operator fun <T : UShort> CPointer<UShortVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("plus\$UInt")
public inline operator fun <T : UIntVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 4)

@ExperimentalForeignApi
@JvmName("plus\$UInt")
public inline operator fun <T : UIntVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@ExperimentalForeignApi
@JvmName("get\$UInt")
public inline operator fun <T : UInt> CPointer<UIntVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
public inline operator fun <T : UInt> CPointer<UIntVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("get\$UInt")
public inline operator fun <T : UInt> CPointer<UIntVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
public inline operator fun <T : UInt> CPointer<UIntVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("plus\$ULong")
public inline operator fun <T : ULongVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 8)

@ExperimentalForeignApi
@JvmName("plus\$ULong")
public inline operator fun <T : ULongVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@ExperimentalForeignApi
@JvmName("get\$ULong")
public inline operator fun <T : ULong> CPointer<ULongVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
public inline operator fun <T : ULong> CPointer<ULongVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("get\$ULong")
public inline operator fun <T : ULong> CPointer<ULongVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
public inline operator fun <T : ULong> CPointer<ULongVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("plus\$Float")
public inline operator fun <T : FloatVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 4)

@ExperimentalForeignApi
@JvmName("plus\$Float")
public inline operator fun <T : FloatVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@ExperimentalForeignApi
@JvmName("get\$Float")
public inline operator fun <T : Float> CPointer<FloatVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Float")
public inline operator fun <T : Float> CPointer<FloatVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("get\$Float")
public inline operator fun <T : Float> CPointer<FloatVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Float")
public inline operator fun <T : Float> CPointer<FloatVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("plus\$Double")
public inline operator fun <T : DoubleVarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? =
        interpretCPointer(this.rawValue + index * 8)

@ExperimentalForeignApi
@JvmName("plus\$Double")
public inline operator fun <T : DoubleVarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? =
        this + index.toLong()

@ExperimentalForeignApi
@JvmName("get\$Double")
public inline operator fun <T : Double> CPointer<DoubleVarOf<T>>.get(index: Int): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Double")
public inline operator fun <T : Double> CPointer<DoubleVarOf<T>>.set(index: Int, value: T) {
    (this + index)!!.pointed.value = value
}

@ExperimentalForeignApi
@JvmName("get\$Double")
public inline operator fun <T : Double> CPointer<DoubleVarOf<T>>.get(index: Long): T =
        (this + index)!!.pointed.value

@ExperimentalForeignApi
@JvmName("set\$Double")
public inline operator fun <T : Double> CPointer<DoubleVarOf<T>>.set(index: Long, value: T) {
    (this + index)!!.pointed.value = value
}

/* Seva: Used to be generated by:

Seva: It probably means the reasoning of this API and is general applicability should be revisited

#!/bin/bash

function gen {
echo "@JvmName(\"plus\\\$$1\")"
echo "public inline operator fun <T : ${1}VarOf<*>> CPointer<T>?.plus(index: Long): CPointer<T>? ="
echo "        interpretCPointer(this.rawValue + index * ${2})"
echo
echo "@JvmName(\"plus\\\$$1\")"
echo "public inline operator fun <T : ${1}VarOf<*>> CPointer<T>?.plus(index: Int): CPointer<T>? ="
echo "        this + index.toLong()"
echo
echo "@JvmName(\"get\\\$$1\")"
echo "public inline operator fun <T : $1> CPointer<${1}VarOf<T>>.get(index: Int): T ="
echo "        (this + index)!!.pointed.value"
echo
echo "@JvmName(\"set\\\$$1\")"
echo "public inline operator fun <T : $1> CPointer<${1}VarOf<T>>.set(index: Int, value: T) {"
echo "    (this + index)!!.pointed.value = value"
echo '}'
echo
echo "@JvmName(\"get\\\$$1\")"
echo "public inline operator fun <T : $1> CPointer<${1}VarOf<T>>.get(index: Long): T ="
echo "        (this + index)!!.pointed.value"
echo
echo "@JvmName(\"set\\\$$1\")"
echo "public inline operator fun <T : $1> CPointer<${1}VarOf<T>>.set(index: Long, value: T) {"
echo "    (this + index)!!.pointed.value = value"
echo '}'
echo
}

gen Byte 1
gen Short 2
gen Int 4
gen Long 8
gen UByte 1
gen UShort 2
gen UInt 4
gen ULong 8
gen Float 4
gen Double 8

 */
