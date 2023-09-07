/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.native.concurrent.*
import kotlinx.cinterop.*

fun main(arr: Array<String>) {
    println(arr.size.toByte() == arr[0].toByte())
    println(arr.size.toUByte() == arr[0].toUByte())
    println(arr.size.toShort() == arr[0].toShort())
    println(arr.size.toUShort() == arr[0].toUShort())
    println(arr.size.toInt() == arr[0].toInt())
    println(arr.size.toUInt() == arr[0].toUInt())
    println(arr.size.toLong() == arr[0].toLong())
    println(arr.size.toULong() == arr[0].toULong())
    println(arr.size.toFloat() == arr[0].toFloat())
    println(arr.size.toDouble() == arr[0].toDouble())
    println(Char(arr.size) == arr[0][0])
    println((arr.size == 1) == (arr[0] == "1")) // Boolean

    memScoped {
        val var1: IntVar = alloc()
        val var2: IntVar = alloc()
        println(var1.ptr.rawValue == var2.ptr.rawValue)
    }

    println(Result.success(arr.size) == Result.success(arr[0].toInt()))
    println(vectorOf(arr.size, arr.size, arr.size, arr.size) == vectorOf(arr[0].toInt(), arr[0].toInt(), arr[0].toInt(), arr[0].toInt()))

    val w1 = Worker.start()
    val w2 = Worker.start()
    println(w1 == w2)

    val f1 = w1.requestTermination()
    val f2 = w2.requestTermination()
    println(f1 == f2)

    f1.result
    f2.result
}
// CHECK-NOT: {{call|invoke}} i64 @"kfun:kotlin#<Long-unbox>
// CHECK-NOT: {{call|invoke}} i64 @"kfun:kotlin#<ULong-unbox>

// CHECK-NOT: {{call|invoke}} i32 @"kfun:kotlin#<Int-unbox>
// CHECK-NOT: {{call|invoke}} i32 @"kfun:kotlin#<UInt-unbox>

// CHECK-NOT: {{call|invoke}} signext i16 @"kfun:kotlin#<Short-unbox>
// CHECK-NOT: {{call|invoke}} zeroext i16 @"kfun:kotlin#<UShort-unbox>

// CHECK-NOT: {{call|invoke}} signext i8 @"kfun:kotlin#<Byte-unbox>
// CHECK-NOT: {{call|invoke}} zeroext i8 @"kfun:kotlin#<UByte-unbox>

// CHECK-NOT: {{call|invoke}} zeroext i16 @"kfun:kotlin#<Char-unbox>
// CHECK-NOT: {{call|invoke}} zeroext i1 @"kfun:kotlin#<Boolean-unbox>

// CHECK-NOT: {{call|invoke}} double @"kfun:kotlin#<Double-unbox>
// CHECK-NOT: {{call|invoke}} float @"kfun:kotlin#<Float-unbox>

// CHECK-NOT: {{call|invoke}} i8* @"kfun:kotlin.native.internal#<NativePtr-unbox>
// CHECK-NOT: {{call|invoke}} i32 @"kfun:kotlin.native.concurrent#<Future-unbox>
// CHECK-NOT: {{call|invoke}} i32 @"kfun:kotlin.native.concurrent#<Worker-unbox>
// CHECK-NOT: {{call|invoke}} <4 x float> @"kfun:kotlin.native#<Vector128-unbox>
// CHECK-NOT: {{call|invoke}} %struct.ObjHeader* @"kfun:kotlin#<Result-unbox>

// On APPLE targets, generated functions <T>ToNSNumber may contain non-converted invocations of unbox functions.
// CHECK-APPLE: {{IntToNSNumber|LongToNSNumber|ByteToNSNumber|ShortToNSNumber|FloatToNSNumber|DoubleToNSNumber}}
