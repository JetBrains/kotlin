package org.lab.mat

import kotlin.native.internal.ExportedBridge

@ExportedBridge("add_int8_t")
fun add(a: Byte, b: Byte): Int = a + b

@ExportedBridge("add_int16_t")
fun add(a: Short, b: Short): Int = a + b

@ExportedBridge("add_int32_t")
fun add(a: Int, b: Int): Int = a + b

@ExportedBridge("add_int64_t")
fun add(a: Long, b: Long): Long = a + b

@ExportedBridge("add_uint8_t")
fun add(a: UByte, b: UByte): UInt = a + b

@ExportedBridge("add_uint16_t")
fun add(a: UShort, b: UShort): UInt = a + b

@ExportedBridge("add_uint32_t")
fun add(a: UInt, b: UInt): UInt = a + b

@ExportedBridge("add_uint64_t")
fun add(a: ULong, b: ULong): ULong = a + b

@ExportedBridge("add_float")
fun add(a: Float, b: Float): Float = a + b

@ExportedBridge("add_double")
fun add(a: Double, b: Double): Double = a + b

@ExportedBridge("logical_or")
fun or(a: Boolean, b: Boolean): Boolean = a || b

@ExportedBridge("float_nan")
fun floatNaN(): Float = Float.NaN

@ExportedBridge("double_nan")
fun doubleNaN(): Double = Double.NaN