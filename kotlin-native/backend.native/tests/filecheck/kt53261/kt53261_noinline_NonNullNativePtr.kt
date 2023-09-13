/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.cinterop.*

// CHECK-LABEL: define {{zeroext i1|i1}} @"kfun:kotlin.native.internal.NonNullNativePtr#equals(kotlin.Any?){}kotlin.Boolean"(i8* %0, %struct.ObjHeader* %1)
// CHECK: call i8* @"kfun:kotlin.native.internal#<NonNullNativePtr-unbox>(kotlin.Any?){}kotlin.native.internal.NonNullNativePtr?"

fun main() = memScoped {
    val var1: IntVar = alloc()
    val var2: IntVar = alloc()
    // The first one is K1, the second one is K2.
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    println(var1.ptr.value as Any == var2.ptr.value as Any)
}
