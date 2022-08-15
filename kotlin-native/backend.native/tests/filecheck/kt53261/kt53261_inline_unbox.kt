/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun main(arr: Array<String>) {
    println(arr[0].toInt() + 1)
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
